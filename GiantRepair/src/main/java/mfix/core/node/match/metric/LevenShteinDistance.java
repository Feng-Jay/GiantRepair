package mfix.core.node.match.metric;

public class LevenShteinDistance {
    private String _pStr;
    private String _bStr;

    public LevenShteinDistance(String pStr, String bStr){
        _pStr = pStr;
        _bStr = bStr;
    }

    public Integer compute(){
        int m = _pStr.length();
        int n = _bStr.length();

        // Create a matrix to store intermediate distances
        int[][] dp = new int[m + 1][n + 1];

        // Initialize the first row and first column of the matrix
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        // Calculate the Levenshtein distance
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (_pStr.charAt(i - 1) == _bStr.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        // The Levenshtein distance is the value in the bottom-right cell of the matrix
        return dp[m][n];
    }
}
