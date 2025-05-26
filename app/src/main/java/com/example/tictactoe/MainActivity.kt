// MainActivity.kt
package com.example.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding

// Define a custom color scheme with more transparent colors
val AppColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),         // Blue for X
    secondary = Color(0xFFE91E63),       // Pink for O
    tertiary = Color(0xFF009688),
    background = Color.Transparent,      // Transparent background
    surface = Color(0x99FFFFFF),         // Semi-transparent white
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212),
    primaryContainer = Color(0xBBBBDEFB), // Semi-transparent blue
    onPrimaryContainer = Color(0xFF0D47A1),
    secondaryContainer = Color(0xBBF8BBD0), // Semi-transparent pink
    onSecondaryContainer = Color(0xFFC2185B),
    surfaceVariant = Color(0x99EEEEEE)    // Semi-transparent gray
)

// Define the app typography
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TicTacToeTheme {
                // Our app content with background image
                BackgroundImageContainer {
                    TicTacToeGame()
                }
            }
        }
    }
}

// Background image container composable
@Composable
fun BackgroundImageContainer(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.background1),
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Content
        content()
    }
}

// Game ViewModel with SavedStateHandle for preserving state
class TicTacToeViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    // Restore saved state or initialize new game
    private val _board = MutableStateFlow(savedStateHandle.get<List<String>>("board") ?: List(9) { "" })
    val board: StateFlow<List<String>> = _board

    private val _currentPlayer = MutableStateFlow(savedStateHandle.get<String>("currentPlayer") ?: "X")
    val currentPlayer: StateFlow<String> = _currentPlayer

    private val _gameStatus = MutableStateFlow(savedStateHandle.get<GameStatus>("gameStatus") ?: GameStatus.PLAYING)
    val gameStatus: StateFlow<GameStatus> = _gameStatus

    private val _winningLine = MutableStateFlow(savedStateHandle.get<List<Int>>("winningLine") ?: emptyList())
    val winningLine: StateFlow<List<Int>> = _winningLine

    // Track marker positions for each player to implement the new game logic
    private val _xPositions = MutableStateFlow(savedStateHandle.get<List<Int>>("xPositions") ?: emptyList<Int>())
    val xPositions: StateFlow<List<Int>> = _xPositions

    private val _oPositions = MutableStateFlow(savedStateHandle.get<List<Int>>("oPositions") ?: emptyList<Int>())
    val oPositions: StateFlow<List<Int>> = _oPositions

    // Track which position is being removed with animation
    private val _removingPosition = MutableStateFlow(savedStateHandle.get<Int?>("removingPosition") ?: null)
    val removingPosition: StateFlow<Int?> = _removingPosition

    // Scoreboard - session-based scores (resets on app kill/launch, not on pause/resume)
    private val _xScore = MutableStateFlow(savedStateHandle.get<Int>("xScore") ?: 0)
    val xScore: StateFlow<Int> = _xScore

    private val _oScore = MutableStateFlow(savedStateHandle.get<Int>("oScore") ?: 0)
    val oScore: StateFlow<Int> = _oScore

    private val winPatterns = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Rows
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Columns
        listOf(0, 4, 8), listOf(2, 4, 6)                   // Diagonals
    )

    // Save state when it changes (but scores will reset on app kill)
    private fun saveState() {
        savedStateHandle["board"] = _board.value
        savedStateHandle["currentPlayer"] = _currentPlayer.value
        savedStateHandle["gameStatus"] = _gameStatus.value
        savedStateHandle["winningLine"] = _winningLine.value
        savedStateHandle["xPositions"] = _xPositions.value
        savedStateHandle["oPositions"] = _oPositions.value
        savedStateHandle["removingPosition"] = _removingPosition.value
        // Save scores for onPause/onResume scenarios, but they'll reset on app kill
        savedStateHandle["xScore"] = _xScore.value
        savedStateHandle["oScore"] = _oScore.value
    }

    // Make a move
    fun makeMove(position: Int) {
        if (_board.value[position].isEmpty() && _gameStatus.value == GameStatus.PLAYING) {
            val newBoard = _board.value.toMutableList()
            val currentPlayerValue = _currentPlayer.value
            newBoard[position] = currentPlayerValue

            // Update the player's positions
            if (currentPlayerValue == "X") {
                val updatedPositions = _xPositions.value.toMutableList()
                updatedPositions.add(position)
                _xPositions.value = updatedPositions

                // Check if this is the fourth marker for X
                if (updatedPositions.size == 4) {
                    // First check if we have a win with all markers including the first one
                    val potentialWin = checkForWin(newBoard)
                    if (potentialWin.first) {
                        // We have a win, so update game status and winning line
                        _gameStatus.value = GameStatus.X_WINS
                        _winningLine.value = potentialWin.second
                        _board.value = newBoard
                        // Update score
                        _xScore.value = _xScore.value + 1
                        saveState()
                        return
                    }

                    // No win, so remove the first marker
                    val firstPosition = updatedPositions.first()
                    _removingPosition.value = firstPosition

                    // Clear the cell
                    newBoard[firstPosition] = ""

                    // Update positions list by removing the first marker
                    _xPositions.value = updatedPositions.drop(1)
                }
            } else {
                val updatedPositions = _oPositions.value.toMutableList()
                updatedPositions.add(position)
                _oPositions.value = updatedPositions

                // Check if this is the fourth marker for O
                if (updatedPositions.size == 4) {
                    // First check if we have a win with all markers including the first one
                    val potentialWin = checkForWin(newBoard)
                    if (potentialWin.first) {
                        // We have a win, so update game status and winning line
                        _gameStatus.value = GameStatus.O_WINS
                        _winningLine.value = potentialWin.second
                        _board.value = newBoard
                        // Update score
                        _oScore.value = _oScore.value + 1
                        saveState()
                        return
                    }

                    // No win, so remove the first marker
                    val firstPosition = updatedPositions.first()
                    _removingPosition.value = firstPosition

                    // Clear the cell
                    newBoard[firstPosition] = ""

                    // Update positions list by removing the first marker
                    _oPositions.value = updatedPositions.drop(1)
                }
            }

            _board.value = newBoard

            // Reset removing position after a short delay
            if (_removingPosition.value != null) {
                // In a real implementation, we'd use a coroutine with a delay here
                // For simplicity, we'll just reset it immediately
                _removingPosition.value = null
            }

            // Check for winner
            checkGameState()

            // Switch player if game is still ongoing
            if (_gameStatus.value == GameStatus.PLAYING) {
                _currentPlayer.value = if (_currentPlayer.value == "X") "O" else "X"
            }

            // Save state after making a move
            saveState()
        }
    }

    // Check for a win with the current board state
    // Returns a pair of (hasWin, winningLine)
    private fun checkForWin(board: List<String>): Pair<Boolean, List<Int>> {
        for (pattern in winPatterns) {
            val (a, b, c) = pattern
            if (board[a] != "" && board[a] == board[b] && board[a] == board[c]) {
                return Pair(true, pattern)
            }
        }
        return Pair(false, emptyList())
    }

    // Check if there's a winner or draw
    private fun checkGameState() {
        // Check for winner
        val winResult = checkForWin(_board.value)
        if (winResult.first) {
            val (_, pattern) = winResult
            val winner = _board.value[pattern[0]]
            _gameStatus.value = if (winner == "X") GameStatus.X_WINS else GameStatus.O_WINS
            _winningLine.value = pattern

            // Update scores
            if (winner == "X") {
                _xScore.value = _xScore.value + 1
            } else {
                _oScore.value = _oScore.value + 1
            }
            return
        }

        // Check for draw - no empty cells left
        if (_board.value.none { it.isEmpty() }) {
            _gameStatus.value = GameStatus.DRAW
        }
    }

    // Reset the game (but keep scores)
    fun resetGame() {
        _board.value = List(9) { "" }
        _currentPlayer.value = "X"
        _gameStatus.value = GameStatus.PLAYING
        _winningLine.value = emptyList()
        _xPositions.value = emptyList()
        _oPositions.value = emptyList()
        _removingPosition.value = null

        // Save state after reset (scores remain unchanged)
        saveState()
    }

    // Reset scores (for new session or manual reset)
    fun resetScores() {
        _xScore.value = 0
        _oScore.value = 0
        saveState()
    }

    // Get the number of markers for the current player
    fun getCurrentPlayerMarkerCount(): Int {
        return if (_currentPlayer.value == "X") _xPositions.value.size else _oPositions.value.size
    }
}

enum class GameStatus {
    PLAYING, X_WINS, O_WINS, DRAW
}

// Add this function that collects StateFlow without lifecycle awareness
@Composable
fun <T> StateFlow<T>.collectAsStateSimple(): State<T> {
    val state = remember { mutableStateOf(value) }
    LaunchedEffect(this) {
        collect { state.value = it }
    }
    return state
}

@Composable
fun TicTacToeGame(viewModel: TicTacToeViewModel = viewModel()) {
    // Using collectAsStateWithLifecycle for lifecycle-aware state collection
    val board by viewModel.board.collectAsStateWithLifecycle()
    val currentPlayer by viewModel.currentPlayer.collectAsStateWithLifecycle()
    val gameStatus by viewModel.gameStatus.collectAsStateWithLifecycle()
    val winningLine by viewModel.winningLine.collectAsStateWithLifecycle()
    val xPositions by viewModel.xPositions.collectAsStateWithLifecycle()
    val oPositions by viewModel.oPositions.collectAsStateWithLifecycle()
    val removingPosition by viewModel.removingPosition.collectAsStateWithLifecycle()
    val xScore by viewModel.xScore.collectAsStateWithLifecycle()
    val oScore by viewModel.oScore.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Game title
            Text(
                text = "Tic Tac Toe",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scoreboard replacing the "Your Turn" indicator
            ScoreBoard(
                xScore = xScore,
                oScore = oScore,
                currentPlayer = currentPlayer,
                gameStatus = gameStatus,
                onResetScores = { viewModel.resetScores() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Game status card
            StatusCard(gameStatus, currentPlayer)

            Spacer(modifier = Modifier.height(24.dp))

            // Game board
            GameBoard(
                board = board,
                winningLine = winningLine,
                removingPosition = removingPosition,
                onCellClick = { position -> viewModel.makeMove(position) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Button row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // New Game button
                Button(
                    onClick = { viewModel.resetGame() },
                    modifier = Modifier
                        .height(50.dp)
                        .weight(1f),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "New Game",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reset Scores button
                OutlinedButton(
                    onClick = { viewModel.resetScores() },
                    modifier = Modifier
                        .height(50.dp)
                        .weight(1f),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Reset Scores",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreBoard(
    xScore: Int,
    oScore: Int,
    currentPlayer: String,
    gameStatus: GameStatus,
    onResetScores: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Scoreboard Title
            Text(
                text = "SCOREBOARD",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scores Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // X Player Score
                ScoreSection(
                    player = "X",
                    score = xScore,
                    isCurrentPlayer = currentPlayer == "X" && gameStatus == GameStatus.PLAYING,
                    playerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // VS Divider
                Text(
                    text = "VS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // O Player Score
                ScoreSection(
                    player = "O",
                    score = oScore,
                    isCurrentPlayer = currentPlayer == "O" && gameStatus == GameStatus.PLAYING,
                    playerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ScoreSection(
    player: String,
    score: Int,
    isCurrentPlayer: Boolean,
    playerColor: Color,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrentPlayer) 1.1f else 1f,
        animationSpec = tween(300),
        label = "playerScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.scale(scale)
    ) {
        // Player coin and indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GameCoinImage(
                player = player,
                modifier = Modifier.size(32.dp),
                showShadow = true
            )

            if (isCurrentPlayer) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "◀",
                    color = playerColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Score
        Text(
            text = score.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = playerColor
        )


    }
}

@Composable
fun StatusCard(gameStatus: GameStatus, currentPlayer: String) {
    // Only show status card when game is not in playing state
    if (gameStatus != GameStatus.PLAYING) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (gameStatus) {
                    GameStatus.X_WINS -> Color(0xCCDCEDC8)  // More transparent
                    GameStatus.O_WINS -> Color(0xCCDCEDC8)  // More transparent
                    GameStatus.DRAW -> Color(0xCCFFECB3)    // More transparent
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    when (gameStatus) {
                        GameStatus.X_WINS -> {
                            GameCoinImage(
                                player = "X",
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(end = 8.dp),
                                showShadow = true
                            )
                            Text(
                                text = "Wins!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        GameStatus.O_WINS -> {
                            GameCoinImage(
                                player = "O",
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(end = 8.dp),
                                showShadow = true
                            )
                            Text(
                                text = "Wins!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        GameStatus.DRAW -> {
                            Text(
                                text = "It's a Draw!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF795548)
                            )
                        }
                        else -> { /* Do nothing for PLAYING state */ }
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoard(
    board: List<String>,
    winningLine: List<Int>,
    removingPosition: Int?,
    onCellClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x5C98A4B6)  // Very transparent gray
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.weight(1f)
                ) {
                    for (col in 0 until 3) {
                        val position = row * 3 + col
                        Cell(
                            value = board[position],
                            isPartOfWinningLine = position in winningLine,
                            isBeingRemoved = position == removingPosition,
                            onClick = { onCellClick(position) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Cell(
    value: String,
    isPartOfWinningLine: Boolean,
    isBeingRemoved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPartOfWinningLine) 1.1f else 1f,
        animationSpec = tween(300),
        label = "cellScale"
    )

    Box(
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPartOfWinningLine)
                    Color(0x55FFEB3B)  // More transparent yellow highlight
                else
                    Color(0xFF2A356E)  // Semi-transparent cell background
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = value.isNotEmpty() && !isBeingRemoved,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300))
        ) {
            GameCoinImage(
                player = value,
                modifier = Modifier
                    .size(60.dp)
                    .padding(8.dp),
                showShadow = true
            )
        }
    }
}

// New composable for displaying game coin images
@Composable
fun GameCoinImage(
    player: String,
    modifier: Modifier = Modifier,
    showShadow: Boolean = false
) {
    val imageResource = when (player) {
        "X" -> R.drawable.coin_x  // Add your X coin image to drawable folder
        "O" -> R.drawable.coin_o  // Add your O coin image to drawable folder
        else -> return // Don't render anything for empty cells
    }

    val coinModifier = if (showShadow) {
        modifier
            .shadow(
                elevation = 4.dp,
                shape = CircleShape
            )
            .clip(CircleShape)
    } else {
        modifier.clip(CircleShape)
    }

    Image(
        painter = painterResource(id = imageResource),
        contentDescription = "$player coin",
        contentScale = ContentScale.Crop
    )
}

// Theme setup with custom color scheme
@Composable
fun TicTacToeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,  // Using custom color scheme
        typography = AppTypography,
        content = content
    )
}

// Composable specifically for previews
@Composable
fun TicTacToeGamePreviewable() {
    // Use regular State instead of StateFlow for previews
    val board = remember { mutableStateOf(List(9) { "" }) }
    val currentPlayer = remember { mutableStateOf("X") }
    val gameStatus = remember { mutableStateOf(GameStatus.PLAYING) }
    val winningLine = remember { mutableStateOf(emptyList<Int>()) }
    val removingPosition = remember { mutableStateOf<Int?>(null) }
    val xScore = remember { mutableStateOf(3) }
    val oScore = remember { mutableStateOf(2) }

    BackgroundImageContainer {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Game title
                Text(
                    text = "Tic Tac Toe",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scoreboard
                ScoreBoard(
                    xScore = xScore.value,
                    oScore = oScore.value,
                    currentPlayer = currentPlayer.value,
                    gameStatus = gameStatus.value,
                    onResetScores = { }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Game status card
                StatusCard(gameStatus.value, currentPlayer.value)

                Spacer(modifier = Modifier.height(24.dp))

                // Game board
                GameBoard(
                    board = board.value,
                    winningLine = winningLine.value,
                    removingPosition = removingPosition.value,
                    onCellClick = { }  // No-op for preview
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Button row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // New Game button
                    Button(
                        onClick = { },  // No-op for preview
                        modifier = Modifier
                            .height(50.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = "New Game",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Reset Scores button
                    OutlinedButton(
                        onClick = { },  // No-op for preview
                        modifier = Modifier
                            .height(50.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Reset Scores",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Different preview options
@Preview(showBackground = true)
@Composable
fun EmptyGamePreview() {
    TicTacToeTheme {
        TicTacToeGamePreviewable()
    }
}

@Preview(showBackground = true)
@Composable
fun GameBoardPreview() {
    TicTacToeTheme {
        BackgroundImageContainer {
            GameBoard(
                board = listOf("X", "O", "X", "", "O", "", "X", "", ""),
                winningLine = listOf(0, 4, 8),
                removingPosition = null,
                onCellClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScoreBoardPreview() {
    TicTacToeTheme {
        BackgroundImageContainer {
            Column(Modifier.padding(16.dp)) {
                ScoreBoard(
                    xScore = 5,
                    oScore = 3,
                    currentPlayer = "X",
                    gameStatus = GameStatus.PLAYING,
                    onResetScores = {}
                )
                Spacer(Modifier.height(16.dp))
                ScoreBoard(
                    xScore = 2,
                    oScore = 4,
                    currentPlayer = "O",
                    gameStatus = GameStatus.O_WINS,
                    onResetScores = {}
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatusCardPreview() {
    TicTacToeTheme {
        BackgroundImageContainer {
            Column(Modifier.padding(16.dp)) {
                StatusCard(GameStatus.PLAYING, "X")
                Spacer(Modifier.height(16.dp))
                StatusCard(GameStatus.X_WINS, "X")
                Spacer(Modifier.height(16.dp))
                StatusCard(GameStatus.O_WINS, "O")
                Spacer(Modifier.height(16.dp))
                StatusCard(GameStatus.DRAW, "X")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoinPreview() {
    TicTacToeTheme {
        BackgroundImageContainer {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GameCoinImage(
                    player = "X",
                    modifier = Modifier.size(80.dp),
                    showShadow = true
                )
                GameCoinImage(
                    player = "O",
                    modifier = Modifier.size(80.dp),
                    showShadow = true
                )
            }
        }
    }
}