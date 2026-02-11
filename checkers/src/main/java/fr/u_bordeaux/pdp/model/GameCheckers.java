public class GameCheckers {  

    private State state;

    private Board board;

    private Player whitePlayer;
    private Player blackPlayer;

    private boolean isWhiteTurn;

    // private HistoryManager historyManager;

    public GameCheckers() {
        state = new InGameState(this);
    }

    public List<Move> getPossibleMoves(Player player) {
        // Calculer d'abord toutes les captures possibles via le module bitboard
        // board.generateCaptures(); : n'existe pas encore
        List<Move> captures = board.generateCaptures();

        // Si une capture est possible, elle est obligatoire 
        if (!captures.isEmpty()) {
            return captures;
        }
        
        // On retourne les déplacements simples 
        // itboardModule.generateSimpleMoves(); : n'existe pas encore
        return board.generateSimpleMoves();
    }

    public boolean isValidMove(Move move) {
        // Vérifier que c'est bien au tour du joueur qui tente de bouger
        if (move.getPlayer() != this.currentPlayer) {
            return false;
        }

        // Récupérer la liste des coups légaux pour ce joueur 
        List<Move> legalMoves = getPossibleMoves(this.currentPlayer);
        
        // Vérifier si le move proposé est dans la liste
        return legalMoves.contains(move);
    }

    public void applyMove(Move move) {
    // Appliquer physiquement le mouvement sur les bitboards 
    // board.updateBitboards(); : n'existe pas encore 
    board.updateBitboards();
    
    // Gérer la promotion : si un pion atteint la dernière ligne, il devient une Dame 
    // board.shouldBePromoted() : n'existe pas encore
    // board.promoteToQueen() : n'existe pas encore
    if (board.shouldBePromoted(move)) {
        board.promoteToQueen(this.board);
    }
    
    // Enregistrer le coup dans l'historique avant de changer de tour
    
    // Changer le tour du joueur
    this.currentPlayer = (this.currentPlayer == whitePlayer) ? blackPlayer : whitePlayer;
    
    // Notifier les vues
    notifyObservers();
}

    public State checkGameOver() {
        // Si le joueur actuel n'a plus de mouvements possibles, il a perdu 
        if (getPossibleMoves(this.currentPlayer).isEmpty()) {
            this.winner = (this.currentPlayer == whitePlayer) ? blackPlayer : whitePlayer;
            return State.FINISHED;
        }
        
        // Vérifier les conditions de match nul (ex: répétition de positions)
        // board.isDraw() : n'existe pas encore
        if (board.isDraw(this.board)) {
            return State.DRAW;
        }
        
        // La partie continue
        return State.INGAME; 
    }
}