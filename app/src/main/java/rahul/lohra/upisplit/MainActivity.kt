package rahul.lohra.upisplit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import rahul.lohra.upisplit.ui.SplitUpiApp
import rahul.lohra.upisplit.ui.theme.UPISplitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UPISplitTheme {
                SplitUpiApp()
            }
        }
    }
}
