package com.chainloom.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pure-Java game rules. No Android dependencies, so it can be stress-tested on the JVM. */
public final class GameEngine {
    public static final int SIZE = 8;
    public static final int TRAY_SIZE = 3;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_DAILY = 1;
    public static final int PULSE_MAX = 100;

    private static final int COLOR_COUNT = 5;
    private static final int BASE_CLUSTER_THRESHOLD = 6;
    public static final int KNOT = 6;

    // Base polyominoes. Rotation is computed and normalized at runtime.
    private static final int[][][] SHAPES = new int[][][] {
        {{0,0},{0,1}},                                  // domino
        {{0,0},{0,1},{0,2}},                            // line 3
        {{0,0},{1,0},{1,1}},                            // L 3
        {{0,0},{0,1},{1,0},{1,1}},                      // square
        {{0,0},{1,0},{2,0},{2,1}},                      // L 4
        {{0,0},{0,1},{0,2},{1,1}},                      // T 4
        {{0,0},{0,1},{1,1},{1,2}},                      // zig 4
        {{0,0},{0,1},{0,2},{0,3}},                      // line 4
        {{0,0},{0,1},{0,2},{1,0},{1,1}},                // P 5
        {{0,1},{1,0},{1,1},{1,2},{2,1}},                // plus 5
        {{0,0},{1,0},{1,1},{2,1},{2,2}},                // stair 5
        {{0,0},{0,1},{0,2},{1,0},{2,0}}                 // big L 5
    };

    /** Small deterministic PRNG with an explicitly serializable state. */
    private static final class DeterministicRandom {
        private long state;

        DeterministicRandom(long seed) {
            state = seed;
        }

        long getState() { return state; }
        void setState(long value) { state = value; }

        private long nextLong() {
            long z = (state += 0x9E3779B97F4A7C15L);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }

        int nextInt(int bound) {
            if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
            return (int) Long.remainderUnsigned(nextLong(), bound);
        }
    }

    public static final class Piece {
        public final int shapeId;
        public final int rotation;
        public final int color;

        public Piece(int shapeId, int rotation, int color) {
            this.shapeId = shapeId;
            this.rotation = ((rotation % 4) + 4) % 4;
            this.color = color;
        }

        public int[][] cells() {
            return rotatedCells(shapeId, rotation);
        }

        public Piece rotated() {
            return new Piece(shapeId, (rotation + 1) % 4, color);
        }

        public int cellCount() {
            return SHAPES[shapeId].length;
        }
    }

    public static final class Move {
        public final int trayIndex;
        public final int row;
        public final int col;

        public Move(int trayIndex, int row, int col) {
            this.trayIndex = trayIndex;
            this.row = row;
            this.col = col;
        }
    }

    public static final class ActionResult {
        public final boolean success;
        public final int cleared;
        public final int cascades;
        public final int scoreGained;
        public final boolean earnedShift;

        private ActionResult(boolean success, int cleared, int cascades, int scoreGained, boolean earnedShift) {
            this.success = success;
            this.cleared = cleared;
            this.cascades = cascades;
            this.scoreGained = scoreGained;
            this.earnedShift = earnedShift;
        }

        public static ActionResult fail() {
            return new ActionResult(false, 0, 0, 0, false);
        }
    }

    private final int[][] board = new int[SIZE][SIZE];
    private final Piece[] tray = new Piece[TRAY_SIZE];
    private DeterministicRandom random;
    private long seed;
    private int mode;
    private int score;
    private int bestCombo;
    private int combo;
    private int pulse;
    private int shifts;
    private int moveCount;
    private int nextShiftScore;
    private boolean gameOver;

    public GameEngine(long seed, int mode) {
        newGame(seed, mode);
    }

    public void newGame(long newSeed, int newMode) {
        for (int r = 0; r < SIZE; r++) Arrays.fill(board[r], 0);
        this.seed = newSeed;
        this.mode = newMode;
        this.random = new DeterministicRandom(newSeed);
        this.score = 0;
        this.bestCombo = 0;
        this.combo = 0;
        this.pulse = 0;
        this.shifts = 1;
        this.moveCount = 0;
        this.nextShiftScore = 2500;
        this.gameOver = false;
        refillTray();
        evaluateEndState();
    }

    public int getCell(int row, int col) { return board[row][col]; }
    public Piece getPiece(int index) { return tray[index]; }
    public int getScore() { return score; }
    public int getCombo() { return combo; }
    public int getBestCombo() { return bestCombo; }
    public int getPulse() { return pulse; }
    public int getShifts() { return shifts; }
    public int getMoveCount() { return moveCount; }
    public int getMode() { return mode; }
    public long getSeed() { return seed; }
    public boolean isGameOver() { return gameOver; }
    public boolean isPulseReady() { return pulse >= PULSE_MAX; }

    public int occupiedCount() {
        int n = 0;
        for (int[] row : board) for (int v : row) if (v != 0) n++;
        return n;
    }

    public Piece rotatePiece(int trayIndex) {
        if (trayIndex < 0 || trayIndex >= TRAY_SIZE || tray[trayIndex] == null) return null;
        tray[trayIndex] = tray[trayIndex].rotated();
        evaluateEndState();
        return tray[trayIndex];
    }

    public boolean canPlace(int trayIndex, int row, int col) {
        if (trayIndex < 0 || trayIndex >= TRAY_SIZE) return false;
        Piece piece = tray[trayIndex];
        if (piece == null) return false;
        return canPlace(piece, row, col);
    }

    public boolean canPlace(Piece piece, int row, int col) {
        if (piece == null) return false;
        for (int[] cell : piece.cells()) {
            int rr = row + cell[0];
            int cc = col + cell[1];
            if (rr < 0 || rr >= SIZE || cc < 0 || cc >= SIZE || board[rr][cc] != 0) return false;
        }
        return true;
    }

    public ActionResult place(int trayIndex, int row, int col) {
        if (gameOver || !canPlace(trayIndex, row, col)) return ActionResult.fail();
        Piece piece = tray[trayIndex];
        int before = score;
        int shiftsBefore = shifts;
        for (int[] cell : piece.cells()) {
            board[row + cell[0]][col + cell[1]] = piece.color;
        }
        score += piece.cellCount() * 5;
        tray[trayIndex] = null;
        moveCount++;
        applyPressureKnotIfDue();

        ResolveResult rr = resolveClears();
        if (rr.cleared > 0) {
            combo++;
            bestCombo = Math.max(bestCombo, combo);
        } else {
            combo = 0;
        }
        awardScoreShiftIfNeeded();

        if (allTrayUsed()) refillTray();
        evaluateEndState();
        return new ActionResult(true, rr.cleared, rr.cascades, score - before, shifts > shiftsBefore);
    }

    /** Spend a full Pulse meter on any occupied cell, clearing that entire connected color group. */
    public ActionResult usePulse(int row, int col) {
        if (gameOver || pulse < PULSE_MAX || row < 0 || row >= SIZE || col < 0 || col >= SIZE || board[row][col] == 0) {
            return ActionResult.fail();
        }
        int before = score;
        int shiftsBefore = shifts;
        List<int[]> group = collectGroup(row, col, board[row][col], null);
        for (int[] p : group) board[p[0]][p[1]] = 0;
        pulse = 0;
        score += group.size() * 10;
        applyGravity();
        ResolveResult rr = resolveClears();
        int totalCleared = group.size() + rr.cleared;
        combo = Math.max(1, combo + 1);
        bestCombo = Math.max(bestCombo, combo);
        awardScoreShiftIfNeeded();
        evaluateEndState();
        return new ActionResult(true, totalCleared, Math.max(1, rr.cascades), score - before, shifts > shiftsBefore);
    }

    /** Redraw the remaining tray. Useful as a strategy tool and as a rescue when no current piece fits. */
    public boolean shiftTray() {
        if (gameOver || shifts <= 0) return false;
        shifts--;
        for (int i = 0; i < TRAY_SIZE; i++) tray[i] = null;
        refillTray();
        evaluateEndState();
        return true;
    }

    public boolean hasAnyMove() {
        for (int i = 0; i < TRAY_SIZE; i++) {
            Piece p = tray[i];
            if (p == null) continue;
            // Rotation is a free player action, so end-state checks must consider every orientation.
            for (int rot = 0; rot < 4; rot++) {
                Piece candidate = new Piece(p.shapeId, rot, p.color);
                for (int r = 0; r < SIZE; r++) {
                    for (int c = 0; c < SIZE; c++) {
                        if (canPlace(candidate, r, c)) return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isStuckButRescuable() {
        return !hasAnyMove() && (shifts > 0 || isPulseReady());
    }

    public List<Move> validMoves() {
        List<Move> result = new ArrayList<Move>();
        for (int i = 0; i < TRAY_SIZE; i++) {
            Piece p = tray[i];
            if (p == null) continue;
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    if (canPlace(p, r, c)) result.add(new Move(i, r, c));
                }
            }
        }
        return result;
    }

    private void evaluateEndState() {
        if (hasAnyMove()) {
            gameOver = false;
            return;
        }
        gameOver = shifts <= 0 && pulse < PULSE_MAX;
    }

    private boolean allTrayUsed() {
        for (Piece p : tray) if (p != null) return false;
        return true;
    }

    private void refillTray() {
        // Fairness rule: avoid a tray that is completely unplayable when a reasonably-sized piece can still fit.
        Piece[] candidate = new Piece[TRAY_SIZE];
        int attempts = 0;
        do {
            for (int i = 0; i < TRAY_SIZE; i++) candidate[i] = randomPiece();
            attempts++;
        } while (attempts < 4 && occupiedCount() > 0 && !anyCandidateFits(candidate) && boardHasRoomForCommonPiece());
        System.arraycopy(candidate, 0, tray, 0, TRAY_SIZE);
    }

    private boolean anyCandidateFits(Piece[] pieces) {
        for (Piece p : pieces) {
            for (int rot = 0; rot < 4; rot++) {
                Piece candidate = new Piece(p.shapeId, rot, p.color);
                for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) if (canPlace(candidate, r, c)) return true;
            }
        }
        return false;
    }

    private boolean boardHasRoomForCommonPiece() {
        // Domino is our smallest normal piece. If no domino orientation fits, a fresh tray should be allowed to end the run.
        Piece h = new Piece(0, 0, 1);
        Piece v = new Piece(0, 1, 1);
        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) {
            if (canPlace(h, r, c) || canPlace(v, r, c)) return true;
        }
        return false;
    }

    private Piece randomPiece() {
        double fill = occupiedCount() / 64.0;
        int shapeId;
        // As pressure rises, modestly bias toward smaller pieces without removing large high-value shapes entirely.
        int roll = random.nextInt(100);
        if (fill > 0.72 && roll < 48) shapeId = random.nextInt(4);       // 2-4 cells
        else if (fill > 0.55 && roll < 28) shapeId = random.nextInt(7);
        else shapeId = random.nextInt(SHAPES.length);
        int rotation = random.nextInt(4);
        int color = 1 + random.nextInt(COLOR_COUNT);
        return new Piece(shapeId, rotation, color);
    }

    private static int[][] rotatedCells(int shapeId, int rotation) {
        int[][] src = SHAPES[shapeId];
        int[][] out = new int[src.length][2];
        for (int i = 0; i < src.length; i++) {
            int r = src[i][0];
            int c = src[i][1];
            int rr, cc;
            switch (rotation & 3) {
                case 1: rr = c; cc = -r; break;
                case 2: rr = -r; cc = -c; break;
                case 3: rr = -c; cc = r; break;
                default: rr = r; cc = c; break;
            }
            out[i][0] = rr;
            out[i][1] = cc;
        }
        int minR = Integer.MAX_VALUE, minC = Integer.MAX_VALUE;
        for (int[] p : out) { minR = Math.min(minR, p[0]); minC = Math.min(minC, p[1]); }
        for (int[] p : out) { p[0] -= minR; p[1] -= minC; }
        Arrays.sort(out, new Comparator<int[]>() {
            @Override public int compare(int[] a, int[] b) {
                if (a[0] != b[0]) return a[0] - b[0];
                return a[1] - b[1];
            }
        });
        return out;
    }

    private static final class ResolveResult {
        int cleared;
        int cascades;
    }

    private ResolveResult resolveClears() {
        ResolveResult total = new ResolveResult();
        int chain = 1;
        while (true) {
            boolean[][] marked = new boolean[SIZE][SIZE];
            int lineCount = markFullLines(marked);
            markLargeColorGroups(marked);
            int n = countMarked(marked);
            if (n == 0) break;
            clearMarked(marked);
            total.cleared += n;
            total.cascades++;
            score += n * 12 * chain + lineCount * 70 * chain;
            int pulsePerTile = moveCount < 180 ? 2 : (moveCount < 300 ? 1 : 0);
            int pulsePerLine = moveCount < 180 ? 5 : (moveCount < 300 ? 3 : 0);
            pulse = Math.min(PULSE_MAX, pulse + n * pulsePerTile + lineCount * pulsePerLine);
            applyGravity();
            chain++;
        }
        return total;
    }

    private int markFullLines(boolean[][] marked) {
        int lines = 0;
        for (int r = 0; r < SIZE; r++) {
            boolean full = true;
            for (int c = 0; c < SIZE; c++) if (board[r][c] == 0) { full = false; break; }
            if (full) { lines++; for (int c = 0; c < SIZE; c++) marked[r][c] = true; }
        }
        for (int c = 0; c < SIZE; c++) {
            boolean full = true;
            for (int r = 0; r < SIZE; r++) if (board[r][c] == 0) { full = false; break; }
            if (full) { lines++; for (int r = 0; r < SIZE; r++) marked[r][c] = true; }
        }
        return lines;
    }

    private int currentClusterThreshold() {
        if (moveCount < 90) return BASE_CLUSTER_THRESHOLD;
        if (moveCount < 170) return BASE_CLUSTER_THRESHOLD + 1;
        if (moveCount < 300) return BASE_CLUSTER_THRESHOLD + 2;
        return BASE_CLUSTER_THRESHOLD + 3;
    }

    private void markLargeColorGroups(boolean[][] marked) {
        boolean[][] seen = new boolean[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0 || board[r][c] == KNOT || seen[r][c]) continue;
                List<int[]> group = collectGroup(r, c, board[r][c], seen);
                if (group.size() >= currentClusterThreshold()) {
                    for (int[] p : group) marked[p[0]][p[1]] = true;
                }
            }
        }
    }

    private List<int[]> collectGroup(int sr, int sc, int color, boolean[][] sharedSeen) {
        boolean[][] seen = sharedSeen == null ? new boolean[SIZE][SIZE] : sharedSeen;
        ArrayDeque<int[]> q = new ArrayDeque<int[]>();
        List<int[]> group = new ArrayList<int[]>();
        q.add(new int[]{sr, sc});
        seen[sr][sc] = true;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!q.isEmpty()) {
            int[] p = q.removeFirst();
            group.add(p);
            for (int[] d : dirs) {
                int r = p[0] + d[0], c = p[1] + d[1];
                if (r >= 0 && r < SIZE && c >= 0 && c < SIZE && !seen[r][c] && board[r][c] == color) {
                    seen[r][c] = true;
                    q.addLast(new int[]{r,c});
                }
            }
        }
        return group;
    }

    private int countMarked(boolean[][] marked) {
        int n = 0;
        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) if (marked[r][c]) n++;
        return n;
    }

    private void clearMarked(boolean[][] marked) {
        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) if (marked[r][c]) board[r][c] = 0;
    }

    private void applyGravity() {
        for (int c = 0; c < SIZE; c++) {
            int write = SIZE - 1;
            for (int r = SIZE - 1; r >= 0; r--) {
                if (board[r][c] != 0) {
                    int v = board[r][c];
                    board[r][c] = 0;
                    board[write][c] = v;
                    write--;
                }
            }
            while (write >= 0) board[write--][c] = 0;
        }
    }


    private void applyPressureKnotIfDue() {
        if (moveCount < 18) return;
        int interval;
        if (moveCount < 45) interval = 12;
        else if (moveCount < 90) interval = 9;
        else if (moveCount < 140) interval = 6;
        else if (moveCount < 220) interval = 4;
        else if (moveCount < 300) interval = 2;
        else interval = 1;
        if (moveCount % interval != 0) return;
        int knotCount = moveCount < 140 ? 1 : (moveCount < 220 ? 2 : (moveCount < 320 ? 3 : 4));
        for (int n = 0; n < knotCount; n++) {
            List<Integer> columns = new ArrayList<Integer>();
            for (int c = 0; c < SIZE; c++) {
                for (int r = 0; r < SIZE; r++) {
                    if (board[r][c] == 0) { columns.add(c); break; }
                }
            }
            if (columns.isEmpty()) return;
            int c = columns.get(random.nextInt(columns.size()));
            // Insert at the highest empty cell in the column; future gravity pulls it into the weave.
            int target = -1;
            for (int r = 0; r < SIZE; r++) {
                if (board[r][c] == 0) { target = r; break; }
            }
            if (target >= 0) board[target][c] = KNOT;
        }
    }

    private void awardScoreShiftIfNeeded() {
        // Score rewards are deliberately finite: rescue tools should save a run, not create an infinite economy.
        while (nextShiftScore <= 8500 && score >= nextShiftScore) {
            shifts = Math.min(3, shifts + 1);
            nextShiftScore += 3000;
        }
        if (nextShiftScore > 8500) nextShiftScore = Integer.MAX_VALUE;
    }

    /** Compact local save format. Versioned so future changes can reject incompatible saves. */
    public String exportState() {
        StringBuilder sb = new StringBuilder();
        sb.append("CL2|").append(seed).append('|').append(mode).append('|').append(score).append('|')
          .append(combo).append('|').append(bestCombo).append('|').append(pulse).append('|').append(shifts).append('|')
          .append(moveCount).append('|').append(nextShiftScore).append('|').append(random.getState()).append('|');
        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) sb.append(board[r][c]);
        sb.append('|');
        for (int i = 0; i < TRAY_SIZE; i++) {
            if (i > 0) sb.append(',');
            Piece p = tray[i];
            if (p == null) sb.append('x');
            else sb.append(p.shapeId).append('.').append(p.rotation).append('.').append(p.color);
        }
        return sb.toString();
    }

    public static GameEngine importState(String state) {
        if (state == null || !state.startsWith("CL2|")) return null;
        try {
            String[] parts = state.split("\\|", -1);
            if (parts.length < 13) return null;
            long seed = Long.parseLong(parts[1]);
            int mode = Integer.parseInt(parts[2]);
            GameEngine e = new GameEngine(seed, mode);
            e.score = Integer.parseInt(parts[3]);
            e.combo = Integer.parseInt(parts[4]);
            e.bestCombo = Integer.parseInt(parts[5]);
            e.pulse = Integer.parseInt(parts[6]);
            e.shifts = Integer.parseInt(parts[7]);
            e.moveCount = Integer.parseInt(parts[8]);
            e.nextShiftScore = Integer.parseInt(parts[9]);
            e.random.setState(Long.parseLong(parts[10]));
            String cells = parts[11];
            if (cells.length() != SIZE * SIZE) return null;
            int k = 0;
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    int v = cells.charAt(k++) - '0';
                    if (v < 0 || v > KNOT) return null;
                    e.board[r][c] = v;
                }
            }
            String[] ps = parts[12].split(",", -1);
            if (ps.length != TRAY_SIZE) return null;
            for (int i = 0; i < TRAY_SIZE; i++) {
                if ("x".equals(ps[i])) e.tray[i] = null;
                else {
                    String[] f = ps[i].split("\\.");
                    if (f.length != 3) return null;
                    int shape = Integer.parseInt(f[0]);
                    int rotation = Integer.parseInt(f[1]);
                    int color = Integer.parseInt(f[2]);
                    if (shape < 0 || shape >= SHAPES.length || color < 1 || color > COLOR_COUNT) return null;
                    e.tray[i] = new Piece(shape, rotation, color);
                }
            }
            e.evaluateEndState();
            return e;
        } catch (RuntimeException ex) {
            return null;
        }
    }

}
