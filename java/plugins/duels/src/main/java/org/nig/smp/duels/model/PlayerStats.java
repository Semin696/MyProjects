package org.nig.smp.duels.model;

public final class PlayerStats {

    private int wins;
    private int losses;
    private int winstreak;
    private int bestWinstreak;

    public PlayerStats() {
    }

    public PlayerStats(int wins, int losses, int winstreak, int bestWinstreak) {
        this.wins = wins;
        this.losses = losses;
        this.winstreak = winstreak;
        this.bestWinstreak = bestWinstreak;
    }

    public void recordWin() {
        wins++;
        winstreak++;
        if (winstreak > bestWinstreak) {
            bestWinstreak = winstreak;
        }
    }

    public void recordLoss() {
        losses++;
        winstreak = 0;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getTotal() {
        return wins + losses;
    }

    public int getWinstreak() {
        return winstreak;
    }

    public int getBestWinstreak() {
        return bestWinstreak;
    }
}
