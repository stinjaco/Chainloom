package com.chainloom.game;

import java.util.List;
import java.util.Random;

public final class GameEngineHarness {
    private static void require(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }

    private static void validate(GameEngine e) {
        for (int r = 0; r < GameEngine.SIZE; r++) for (int c = 0; c < GameEngine.SIZE; c++) {
            int v = e.getCell(r,c);
            require(v >= 0 && v <= GameEngine.KNOT, "bad board value");
        }
        for (int i = 0; i < GameEngine.TRAY_SIZE; i++) {
            GameEngine.Piece p = e.getPiece(i);
            if (p == null) continue;
            require(p.color >= 1 && p.color <= 5, "bad color");
            for (int[] cell : p.cells()) require(cell[0] >= 0 && cell[1] >= 0, "unnormalized piece");
        }
        if (e.isGameOver()) require(!e.hasAnyMove(), "game over despite valid move");
    }

    private static long[] simulate(long seed, boolean useTools) {
        GameEngine e = new GameEngine(seed, GameEngine.MODE_NORMAL);
        Random rnd = new Random(seed ^ 0xBADC0FFEE0DDF00DL);
        int safety = 0;
        while (!e.isGameOver() && safety++ < 2000) {
            validate(e);
            List<GameEngine.Move> moves = e.validMoves();
            if (moves.isEmpty() && e.hasAnyMove()) {
                // A free rotation may be the only legal continuation. Rotate deterministically until one appears.
                boolean found = false;
                for (int i = 0; i < GameEngine.TRAY_SIZE && !found; i++) {
                    if (e.getPiece(i) == null) continue;
                    for (int rot = 0; rot < 3 && !found; rot++) {
                        e.rotatePiece(i);
                        moves = e.validMoves();
                        found = !moves.isEmpty();
                    }
                }
            }
            if (moves.isEmpty()) {
                if (useTools && e.isPulseReady()) {
                    boolean done = false;
                    for (int r = GameEngine.SIZE - 1; r >= 0 && !done; r--) for (int c = 0; c < GameEngine.SIZE && !done; c++) {
                        if (e.getCell(r,c) != 0) done = e.usePulse(r,c).success;
                    }
                    if (done) continue;
                }
                if (useTools && e.getShifts() > 0 && e.shiftTray()) continue;
                break;
            }
            // Random player with occasional rotation; validMoves already includes current tray rotations.
            GameEngine.Move m = moves.get(rnd.nextInt(moves.size()));
            GameEngine.ActionResult ar = e.place(m.trayIndex, m.row, m.col);
            require(ar.success, "valid move failed");
            if (rnd.nextInt(9) == 0) e.rotatePiece(rnd.nextInt(GameEngine.TRAY_SIZE));
        }
        require(safety < 2000, "run did not converge seed="+seed+" moves="+e.getMoveCount()+" score="+e.getScore()+" occ="+e.occupiedCount()+" shifts="+e.getShifts()+" pulse="+e.getPulse());
        validate(e);
        return new long[]{e.getMoveCount(), e.getScore(), e.getBestCombo()};
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "basic" : args[0];
        if ("basic".equals(mode)) {
            for (int i = 0; i < 100; i++) simulate(1000L + i, true);
            System.out.println("PASS basic invariants: 100 games");
        } else if ("stats".equals(mode) || "statsNoTools".equals(mode)) {
            boolean tools = "stats".equals(mode);
            int games = args.length > 1 ? Integer.parseInt(args[1]) : 500;
            long moves=0, score=0, combo=0, minMoves=Long.MAX_VALUE, maxMoves=0;
            for (int i = 0; i < games; i++) {
                long[] s = simulate(50000L + i, tools);
                moves += s[0]; score += s[1]; combo += s[2];
                minMoves = Math.min(minMoves, s[0]); maxMoves = Math.max(maxMoves, s[0]);
            }
            System.out.println("games="+games+" avgMoves="+(moves/(double)games)+" minMoves="+minMoves+" maxMoves="+maxMoves+
                    " avgScore="+(score/(double)games)+" avgBestCombo="+(combo/(double)games));
        } else if ("determinism".equals(mode)) {
            GameEngine a = new GameEngine(123456789L, GameEngine.MODE_DAILY);
            GameEngine b = new GameEngine(123456789L, GameEngine.MODE_DAILY);
            for (int turn = 0; turn < 80; turn++) {
                require(a.exportState().equals(b.exportState()), "same seed diverged before turn "+turn);
                List<GameEngine.Move> am = a.validMoves();
                List<GameEngine.Move> bm = b.validMoves();
                require(am.size() == bm.size(), "move list size diverged");
                if (am.isEmpty()) break;
                GameEngine.Move ma = am.get(am.size()/2);
                GameEngine.Move mb = bm.get(bm.size()/2);
                require(ma.trayIndex==mb.trayIndex && ma.row==mb.row && ma.col==mb.col, "move ordering diverged");
                a.place(ma.trayIndex, ma.row, ma.col);
                b.place(mb.trayIndex, mb.row, mb.col);
            }
            require(a.exportState().equals(b.exportState()), "same seed final state diverged");
            System.out.println("PASS deterministic daily seed");
        } else if ("save".equals(mode)) {
            GameEngine e = new GameEngine(777, GameEngine.MODE_NORMAL);
            for (int i = 0; i < 12 && !e.validMoves().isEmpty(); i++) {
                GameEngine.Move m = e.validMoves().get(0);
                e.place(m.trayIndex, m.row, m.col);
            }
            String saved = e.exportState();
            GameEngine b = GameEngine.importState(saved);
            require(b != null, "save import null");
            require(e.getScore() == b.getScore(), "score mismatch");
            require(e.getMoveCount() == b.getMoveCount(), "move mismatch");
            require(e.getPulse() == b.getPulse(), "pulse mismatch");
            require(saved.equals(b.exportState()), "serialized state mismatch after round trip");
            for (int r=0;r<GameEngine.SIZE;r++) for(int c=0;c<GameEngine.SIZE;c++) require(e.getCell(r,c)==b.getCell(r,c), "board mismatch");
            System.out.println("PASS save round trip");
        } else if ("resumeDeterminism".equals(mode)) {
            GameEngine uninterrupted = new GameEngine(987654321L, GameEngine.MODE_DAILY);
            for (int turn = 0; turn < 17 && !uninterrupted.isGameOver(); turn++) {
                List<GameEngine.Move> moves = uninterrupted.validMoves();
                if (moves.isEmpty()) break;
                GameEngine.Move m = moves.get(moves.size()/3);
                uninterrupted.place(m.trayIndex, m.row, m.col);
            }
            GameEngine resumed = GameEngine.importState(uninterrupted.exportState());
            require(resumed != null, "resume import null");
            require(uninterrupted.exportState().equals(resumed.exportState()), "state differs at resume boundary");
            for (int turn = 0; turn < 120; turn++) {
                require(uninterrupted.exportState().equals(resumed.exportState()), "resumed RNG diverged before turn "+turn);
                List<GameEngine.Move> a = uninterrupted.validMoves();
                List<GameEngine.Move> bMoves = resumed.validMoves();
                require(a.size() == bMoves.size(), "resumed move count diverged");
                if (a.isEmpty()) {
                    if (uninterrupted.getShifts() > 0) {
                        require(uninterrupted.shiftTray() == resumed.shiftTray(), "shift result diverged");
                        continue;
                    }
                    break;
                }
                GameEngine.Move ma = a.get(a.size()/2), mb = bMoves.get(bMoves.size()/2);
                require(ma.trayIndex==mb.trayIndex && ma.row==mb.row && ma.col==mb.col, "resumed move ordering diverged");
                uninterrupted.place(ma.trayIndex, ma.row, ma.col);
                resumed.place(mb.trayIndex, mb.row, mb.col);
            }
            require(uninterrupted.exportState().equals(resumed.exportState()), "resumed final state diverged");
            System.out.println("PASS deterministic continuation after save/resume");
        } else {
            throw new IllegalArgumentException("unknown mode");
        }
    }
}
