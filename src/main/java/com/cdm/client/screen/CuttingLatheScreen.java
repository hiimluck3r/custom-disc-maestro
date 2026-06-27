package com.cdm.client.screen;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.cdm.audio.NoteInstruments;
import com.cdm.block.entity.CuttingLatheBlockEntity;
import com.cdm.data.DiscMeta;
import com.cdm.data.NoteSequence;
import com.cdm.menu.CuttingLatheMenu;
import com.cdm.nbs.NbsImport;
import com.cdm.nbs.NbsParser;
import com.cdm.net.LatheRecordPayload;
import com.cdm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Cutting Lathe editor: a scrollable piano-roll. Columns are 16th-note time steps (length limited only
 * by the song length, scroll horizontally), rows are pitch shown as piano keys. Pick an instrument,
 * click/drag cells to paint notes, set a free BPM, Play to audition with a moving playhead, then Burn
 * to cut the melody onto a master disc (consumes the blank disc in the input slot). {@code .nbs} files
 * import straight onto the roll.
 */
public class CuttingLatheScreen extends AbstractContainerScreen<CuttingLatheMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath("cdm", "textures/gui/cutting_lathe.png");
    // The editor texture covers the top TEX_H; the slot + inventory panel below is drawn procedurally.
    private static final int GUI_W = 256, TEX_H = 230, GUI_H = 340;

    // Worded purpose for the lathe's two item slots (keyed by menu slot index), shown on hover.
    private static final java.util.Map<Integer, Component> SLOT_HINTS = java.util.Map.of(
            CuttingLatheBlockEntity.SLOT_BLANK, Component.translatable("cdm.lathe.hint.blank"),
            CuttingLatheBlockEntity.SLOT_OUTPUT, Component.translatable("cdm.lathe.hint.master"));

    // Piano roll geometry.
    private static final int KEY_X = 6, GRID_X = 22, GRID_Y = 19;
    private static final int CELL_W = 6, CELL_H = 5, ROWS = 25, VIS_COLS = 38;
    private static final int GRID_BOTTOM = GRID_Y + ROWS * CELL_H; // 144

    // Footer widget rows.
    private static final int SCROLL_Y = 152, SCROLL_H = 5;
    private static final int PAL_X = 8, PAL_Y = 170, PAL_SW = 14, PAL_SH = 11, PAL_STEP = 15;
    private static final int CTRL_Y = 184;
    private static final int BPM_MINUS_X = 44, BPM_PLUS_X = 62, BPM_BTN_W = 16, BPM_BTN_H = 14;
    private static final int BPM_LABEL_X = 82;
    private static final int FIELD1_Y = 200, FIELD2_Y = 213, FIELD_H = 12;

    private static final int MAX_STEPS = 6000;
    private static final int MIN_BPM = 40, MAX_BPM = 300, DEFAULT_BPM = 120;
    private static final int[] INSTRUMENT_COLORS = {
            0xC59A6B, 0x8B4A2B, 0xE0E0E0, 0xB0B0B0, 0x6A8CFF, 0x9AD1B0, 0xF5D06B, 0xC08A4A,
            0xF0A0C0, 0xB0E0A0, 0x90C0C0, 0xD0B080, 0x806040, 0xA060C0, 0xC0A060, 0xE08080
    };

    // Drag modes for the mouse.
    private static final int DRAG_NONE = 0, DRAG_SCROLL = 1, DRAG_ADD = 2, DRAG_REMOVE = 3;

    /** Composed notes, stored by STEP index (tick field = step); baked to game ticks on burn. */
    private final List<NoteSequence.Note> notes = new ArrayList<>();
    private int currentInstrument = 0;
    private int bpm = DEFAULT_BPM;
    private int scrollStep = 0;

    private int dragMode = DRAG_NONE;
    private int lastPaintStep = -1, lastPaintRow = -1;
    private int bpmHoldDir = 0, bpmHoldTicks = 0;

    // Preview playback.
    private boolean playing = false;
    private int playStep = 0;
    private double playElapsed = 0;

    private EditBox titleBox, authorBox, albumBox;
    private Button playButton;

    public CuttingLatheScreen(CuttingLatheMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
    }

    /** Game ticks per 16th-note step at the current tempo. */
    private double ticksPerStep() {
        return 300.0 / bpm;
    }

    @Override
    protected void init() {
        super.init();
        titleBox = textField(8, FIELD1_Y, 118, Component.translatable("cdm.lathe.title"));
        authorBox = textField(130, FIELD1_Y, 118, Component.translatable("cdm.lathe.author"));
        albumBox = textField(8, FIELD2_Y, 240, Component.translatable("cdm.lathe.album"));

        playButton = Button.builder(playLabel(), b -> togglePlay())
                .bounds(leftPos + 8, topPos + CTRL_Y, 34, 14).build();
        addRenderableWidget(playButton);
        // (BPM -/+ are custom hit areas drawn in renderBg, for hold-to-repeat.)
        addRenderableWidget(Button.builder(Component.translatable("cdm.lathe.clear"), b -> { notes.clear(); stop(); })
                .bounds(leftPos + 124, topPos + CTRL_Y, 34, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("cdm.lathe.import"), b -> importNbs())
                .bounds(leftPos + 160, topPos + CTRL_Y, 42, 14)
                .tooltip(Tooltip.create(Component.translatable("cdm.lathe.import_tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("cdm.lathe.burn"), b -> burn())
                .bounds(leftPos + 204, topPos + CTRL_Y, 44, 14)
                .tooltip(Tooltip.create(Component.translatable("cdm.lathe.burn_tip"))).build());
    }

    private EditBox textField(int x, int y, int w, Component label) {
        EditBox box = new EditBox(this.font, leftPos + x, topPos + y, w, FIELD_H, label);
        box.setMaxLength(DiscMeta.MAX_LEN);
        box.setHint(label);
        addRenderableWidget(box);
        return box;
    }

    private Component playLabel() {
        return Component.translatable(playing ? "cdm.lathe.stop" : "cdm.lathe.play");
    }

    // ------------------------------------------------------------------ composing

    private int maxStep() {
        int m = 0;
        for (NoteSequence.Note n : notes) m = Math.max(m, n.tick());
        return m;
    }

    private int scrollMax() {
        int content = Math.min(MAX_STEPS, maxStep() + 1 + VIS_COLS);
        return Math.max(0, content - VIS_COLS);
    }

    private int noteIndex(int step, int note, int instrument) {
        for (int i = 0; i < notes.size(); i++) {
            NoteSequence.Note n = notes.get(i);
            if (n.tick() == step && n.note() == note && n.instrument() == instrument) return i;
        }
        return -1;
    }

    private void preview(int instrument, int note) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    NoteInstruments.sound(instrument).value(), NoteInstruments.pitch(note)));
        }
    }

    private void adjustBpm(int delta) {
        bpm = Math.max(MIN_BPM, Math.min(MAX_BPM, bpm + delta));
    }

    // ------------------------------------------------------------------ preview playback

    private void togglePlay() {
        if (playing) {
            stop();
        } else {
            if (notes.isEmpty()) return;
            playing = true;
            playStep = 0;
            playElapsed = 0;
            scrollStep = 0;
            playStepNotes(0);
        }
        rebuildPlayButton();
    }

    private void stop() {
        playing = false;
        rebuildPlayButton();
    }

    private void rebuildPlayButton() {
        if (playButton != null) playButton.setMessage(playLabel());
    }

    private void playStepNotes(int step) {
        for (NoteSequence.Note n : notes) {
            if (n.tick() == step) preview(n.instrument(), n.note());
        }
        if (step < scrollStep || step >= scrollStep + VIS_COLS) {
            scrollStep = Math.max(0, Math.min(scrollMax(), step - 2));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // BPM hold-to-repeat with acceleration.
        if (bpmHoldDir != 0) {
            bpmHoldTicks++;
            if (bpmHoldTicks > 6) {
                int speed = bpmHoldTicks > 26 ? 5 : bpmHoldTicks > 16 ? 3 : 1;
                adjustBpm(bpmHoldDir * speed);
            }
        }
        if (!playing) return;
        double tps = ticksPerStep();
        playElapsed += 1.0;
        int target = (int) Math.floor(playElapsed / tps);
        while (playStep < target) {
            playStep++;
            playStepNotes(playStep);
        }
        if (playStep > maxStep()) stop();
    }

    // ------------------------------------------------------------------ .nbs import

    private void importNbs() {
        Thread thread = new Thread(() -> {
            String path;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.nbs"));
                filters.flip();
                path = TinyFileDialogs.tinyfd_openFileDialog(
                        "Select a .nbs file", "", filters, "Note Block Studio (*.nbs)", false);
            } catch (Throwable t) {
                feedback(Component.translatable("cdm.lathe.import_failed"));
                return;
            }
            if (path == null) return; // cancelled
            try {
                File file = new File(path);
                if (!file.isFile() || file.length() <= 0 || file.length() > NbsParser.MAX_FILE_BYTES) {
                    feedback(Component.translatable("cdm.lathe.import_too_big"));
                    return;
                }
                byte[] bytes = Files.readAllBytes(file.toPath());
                Optional<NbsImport> result = NbsParser.parse(bytes);
                if (minecraft != null) minecraft.execute(() -> applyImport(result));
            } catch (Throwable t) {
                feedback(Component.translatable("cdm.lathe.import_failed"));
            }
        }, "cdm-nbs-import");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyImport(Optional<NbsImport> result) {
        if (minecraft != null && minecraft.screen != this) return; // screen closed while reading
        if (result.isEmpty()) {
            feedback(Component.translatable("cdm.lathe.import_failed"));
            return;
        }
        NbsImport imp = result.get();
        stop();
        notes.clear();
        double tps = ticksPerStep();
        for (NoteSequence.Note n : imp.sequence().notes()) {
            int s = Math.min(MAX_STEPS - 1, (int) Math.round(n.tick() / tps));
            notes.add(new NoteSequence.Note(s, n.instrument(), n.note()));
        }
        scrollStep = 0;
        if (titleBox.getValue().isBlank() && !imp.songName().isBlank()) titleBox.setValue(imp.songName());
        if (authorBox.getValue().isBlank() && !imp.songAuthor().isBlank()) authorBox.setValue(imp.songAuthor());
        feedback(Component.translatable("cdm.lathe.import_ok", notes.size()));
    }

    private void feedback(Component message) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    private void burn() {
        if (notes.isEmpty()) return;
        double tps = ticksPerStep();
        List<NoteSequence.Note> baked = new ArrayList<>(notes.size());
        int maxStep = 0;
        for (NoteSequence.Note n : notes) {
            baked.add(new NoteSequence.Note((int) Math.round(n.tick() * tps), n.instrument(), n.note()));
            maxStep = Math.max(maxStep, n.tick());
        }
        NoteSequence seq = new NoteSequence((int) Math.round((maxStep + 1) * tps), baked);
        String album = albumBox.getValue().isBlank() ? null : albumBox.getValue();
        DiscMeta meta = new DiscMeta(titleBox.getValue(), authorBox.getValue(), Optional.ofNullable(album));
        PacketDistributor.sendToServer(new LatheRecordPayload(menu.getPos(), seq, meta));
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double rx = mx - leftPos, ry = my - topPos;
        if (button == 0 && inGrid(rx, ry)) {
            int step = scrollStep + (int) Math.floor((rx - GRID_X) / (double) CELL_W);
            int row = (int) Math.floor((ry - GRID_Y) / (double) CELL_H);
            int note = 24 - row;
            int idx = noteIndex(step, note, currentInstrument);
            if (idx >= 0) {
                notes.remove(idx);
                dragMode = DRAG_REMOVE;
            } else if (step >= 0 && step < MAX_STEPS) {
                notes.add(new NoteSequence.Note(step, currentInstrument, note));
                preview(currentInstrument, note);
                dragMode = DRAG_ADD;
            }
            lastPaintStep = step;
            lastPaintRow = row;
            return true;
        }
        if (button == 0 && ry >= PAL_Y && ry < PAL_Y + PAL_SH) {
            int idx = (int) Math.floor((rx - PAL_X) / (double) PAL_STEP);
            if (idx >= 0 && idx < 16 && rx < PAL_X + 16 * PAL_STEP) {
                currentInstrument = idx;
                preview(idx, 12);
                return true;
            }
        }
        if (button == 0 && inRect(rx, ry, BPM_MINUS_X, CTRL_Y, BPM_BTN_W, BPM_BTN_H)) {
            adjustBpm(-1);
            bpmHoldDir = -1;
            bpmHoldTicks = 0;
            return true;
        }
        if (button == 0 && inRect(rx, ry, BPM_PLUS_X, CTRL_Y, BPM_BTN_W, BPM_BTN_H)) {
            adjustBpm(1);
            bpmHoldDir = 1;
            bpmHoldTicks = 0;
            return true;
        }
        if (button == 0 && ry >= SCROLL_Y && ry < SCROLL_Y + SCROLL_H && rx >= GRID_X && rx <= GRID_X + VIS_COLS * CELL_W) {
            dragMode = DRAG_SCROLL;
            scrollTo(rx);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean inGrid(double rx, double ry) {
        return rx >= GRID_X && rx < GRID_X + VIS_COLS * CELL_W && ry >= GRID_Y && ry < GRID_BOTTOM;
    }

    private static boolean inRect(double rx, double ry, int x, int y, int w, int h) {
        return rx >= x && rx < x + w && ry >= y && ry < y + h;
    }

    private void scrollTo(double rx) {
        double frac = (rx - GRID_X) / (double) (VIS_COLS * CELL_W);
        scrollStep = Math.max(0, Math.min(scrollMax(), (int) Math.round(frac * scrollMax())));
    }

    private void paintAt(double mx, double my) {
        double rx = mx - leftPos, ry = my - topPos;
        if (!inGrid(rx, ry)) return;
        int step = scrollStep + (int) Math.floor((rx - GRID_X) / (double) CELL_W);
        int row = (int) Math.floor((ry - GRID_Y) / (double) CELL_H);
        if (step == lastPaintStep && row == lastPaintRow) return; // same cell as last paint
        lastPaintStep = step;
        lastPaintRow = row;
        int note = 24 - row;
        int idx = noteIndex(step, note, currentInstrument);
        if (dragMode == DRAG_ADD && idx < 0 && step >= 0 && step < MAX_STEPS) {
            notes.add(new NoteSequence.Note(step, currentInstrument, note));
            preview(currentInstrument, note);
        } else if (dragMode == DRAG_REMOVE && idx >= 0) {
            notes.remove(idx);
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragMode == DRAG_SCROLL) {
            scrollTo(mx - leftPos);
            return true;
        }
        if (dragMode == DRAG_ADD || dragMode == DRAG_REMOVE) {
            paintAt(mx, my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragMode = DRAG_NONE;
        bpmHoldDir = 0;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        // While a text field is focused, let it swallow keys so 'e' (and others) don't close the menu.
        for (EditBox box : List.of(titleBox, authorBox, albumBox)) {
            if (box.canConsumeInput()) {
                box.keyPressed(key, scan, mods);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dxs, double dys) {
        double ry = my - topPos;
        if (ry >= GRID_Y && ry < GRID_BOTTOM) {
            scrollStep = Math.max(0, Math.min(scrollMax(), scrollStep - (int) Math.signum(dys) * 4));
            return true;
        }
        return super.mouseScrolled(mx, my, dxs, dys);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        gg.blit(BG, leftPos, topPos, 0.0F, 0.0F, GUI_W, TEX_H, GUI_W, TEX_H);
        drawSlotPanel(gg);
        int gx = leftPos + GRID_X, gy = topPos + GRID_Y;
        int gridBottomAbs = topPos + GRID_BOTTOM;

        for (int r = 0; r < ROWS; r++) {
            int note = 24 - r;
            boolean black = isBlackKey(note);
            int ky = gy + r * CELL_H;
            gg.fill(leftPos + KEY_X, ky, gx - 1, ky + CELL_H, black ? 0xFF2B2B33 : 0xFFD8D8D0);
            if (black) gg.fill(gx, ky, gx + VIS_COLS * CELL_W, ky + CELL_H, 0x18000000);
        }
        for (int c = 0; c <= VIS_COLS; c++) {
            int step = scrollStep + c;
            int cx = gx + c * CELL_W;
            int col = (step % 16 == 0) ? 0x40FFFFFF : (step % 4 == 0) ? 0x28FFFFFF : 0x12FFFFFF;
            gg.fill(cx, gy, cx + 1, gridBottomAbs, col);
        }
        for (NoteSequence.Note n : notes) {
            int c = n.tick() - scrollStep;
            int row = 24 - n.note();
            if (c < 0 || c >= VIS_COLS || row < 0 || row >= ROWS) continue;
            int cx = gx + c * CELL_W, cy = gy + row * CELL_H;
            gg.fill(cx + 1, cy + 1, cx + CELL_W, cy + CELL_H, 0xFF000000 | INSTRUMENT_COLORS[n.instrument() % 16]);
            gg.fill(cx + 1, cy + 1, cx + CELL_W, cy + 2, 0x40FFFFFF);
        }
        if (playing) {
            int c = playStep - scrollStep;
            if (c >= 0 && c < VIS_COLS) {
                int px = gx + c * CELL_W;
                gg.fill(px, gy, px + 1, gridBottomAbs, 0xFFFFE066);
            }
        }
        // Instrument palette.
        for (int i = 0; i < 16; i++) {
            int sx = leftPos + PAL_X + i * PAL_STEP, sy = topPos + PAL_Y;
            gg.fill(sx - 1, sy - 1, sx + PAL_SW + 1, sy + PAL_SH + 1, i == currentInstrument ? 0xFFFFE066 : 0xFF101218);
            gg.fill(sx, sy, sx + PAL_SW, sy + PAL_SH, 0xFF000000 | INSTRUMENT_COLORS[i]);
        }
        // BPM -/+ buttons.
        drawMiniButton(gg, leftPos + BPM_MINUS_X, topPos + CTRL_Y, "-");
        drawMiniButton(gg, leftPos + BPM_PLUS_X, topPos + CTRL_Y, "+");
        // Scrollbar.
        int trackX = gx, trackW = VIS_COLS * CELL_W, ty = topPos + SCROLL_Y;
        gg.fill(trackX, ty, trackX + trackW, ty + SCROLL_H, 0xFF101218);
        int range = scrollMax() + VIS_COLS;
        int thumbW = Math.max(12, trackW * VIS_COLS / Math.max(1, range));
        int thumbX = trackX + (trackW - thumbW) * scrollStep / Math.max(1, scrollMax());
        gg.fill(thumbX, ty, thumbX + thumbW, ty + SCROLL_H, 0xFF8A93A8);
    }

    /** Draws the procedural panel below the editor texture: the two lathe slots and the player inventory. */
    private void drawSlotPanel(GuiGraphics gg) {
        int top = topPos + TEX_H;
        gg.fill(leftPos, top, leftPos + GUI_W, topPos + GUI_H, 0xFF1E222C);
        gg.fill(leftPos + 3, top, leftPos + GUI_W - 3, topPos + GUI_H - 3, 0xFF2A3140);

        int blankX = leftPos + CuttingLatheMenu.BLANK_X, blankY = topPos + CuttingLatheMenu.BLANK_Y;
        int outX = leftPos + CuttingLatheMenu.OUTPUT_X, outY = topPos + CuttingLatheMenu.OUTPUT_Y;
        SlotIcons.drawSlot(gg, blankX, blankY);
        SlotIcons.drawSlot(gg, outX, outY);
        gg.drawString(this.font, ">", blankX + 20, blankY + 4, 0xFFB8C0D0, false);
        if (!menu.slots.get(CuttingLatheBlockEntity.SLOT_BLANK).hasItem()) {
            SlotIcons.ghost(gg, new ItemStack(ModItems.BLANK_DISC.get()), blankX, blankY);
        }

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                SlotIcons.drawSlot(gg, leftPos + CuttingLatheMenu.INV_X + c * 18, topPos + CuttingLatheMenu.INV_Y + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            SlotIcons.drawSlot(gg, leftPos + CuttingLatheMenu.INV_X + c * 18, topPos + CuttingLatheMenu.HOTBAR_Y);
        }
    }

    private void drawMiniButton(GuiGraphics gg, int x, int y, String label) {
        gg.fill(x, y, x + BPM_BTN_W, y + BPM_BTN_H, 0xFF20242E);
        gg.fill(x, y, x + BPM_BTN_W, y + 1, 0xFF4A5266);
        gg.fill(x, y, x + 1, y + BPM_BTN_H, 0xFF4A5266);
        gg.fill(x, y + BPM_BTN_H - 1, x + BPM_BTN_W, y + BPM_BTN_H, 0xFF101218);
        gg.drawString(this.font, label, x + (BPM_BTN_W - this.font.width(label)) / 2 + 1, y + 3, 0xFFE8E8E8, false);
    }

    private static boolean isBlackKey(int note) {
        int pc = Math.floorMod(note, 12);
        return pc == 1 || pc == 3 || pc == 6 || pc == 8 || pc == 10;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (ry >= PAL_Y && ry < PAL_Y + PAL_SH) {
            int idx = (int) Math.floor((rx - PAL_X) / (double) PAL_STEP);
            if (idx >= 0 && idx < 16 && rx < PAL_X + 16 * PAL_STEP) {
                gg.renderTooltip(this.font, Component.literal(INSTRUMENT_NAMES[idx]), mouseX, mouseY);
            }
        }
        this.renderTooltip(gg, mouseX, mouseY);
        SlotIcons.emptyHint(gg, this.hoveredSlot, mouseX, mouseY, SLOT_HINTS);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        gg.drawString(this.font, this.title, GRID_X, 5, 0xFFE8E8E8, false);
        gg.drawString(this.font, Component.literal(INSTRUMENT_NAMES[currentInstrument]), PAL_X, PAL_Y - 10, 0xFFE8E8E8, false);
        gg.drawString(this.font, Component.translatable("cdm.lathe.notes", notes.size()),
                PAL_X + 110, PAL_Y - 10, 0xFFB8C0D0, false);
        gg.drawString(this.font, Component.translatable("cdm.lathe.tempo", bpm), BPM_LABEL_X, CTRL_Y + 3, 0xFFE8E8E8, false);
        gg.drawString(this.font, this.playerInventoryTitle, CuttingLatheMenu.INV_X, CuttingLatheMenu.INV_Y - 10, 0xFFB8C0D0, false);
    }

    /** Display names for note-block instruments 0..15 (matches {@link NoteInstruments} order). */
    private static final String[] INSTRUMENT_NAMES = {
            "Harp", "Bass Drum", "Snare", "Hat", "Bass", "Flute", "Bell", "Guitar",
            "Chime", "Xylophone", "Iron Xylophone", "Cow Bell", "Didgeridoo", "Bit", "Banjo", "Pling"
    };
}
