package com.fpf.smartscan.ui.screens.donate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R

@Composable
fun DonateScreen() {
    val clipboardManager = LocalClipboardManager.current
    val btcWallet = stringResource(R.string.btc_wallet)
    val ethWallet = stringResource(R.string.eth_wallet)
    val ltcWallet = stringResource(R.string.ltc_wallet)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Donate to support the project and help drive continuous improvements, new features, and community support!",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        CryptoWalletOption(
            coinName = stringResource(R.string.label_btc),
            walletAddress = btcWallet,
            onCopyClick = {
                clipboardManager.setText(AnnotatedString(btcWallet))
            }
        )
        CryptoWalletOption(
            coinName = stringResource(R.string.label_eth),
            walletAddress = ethWallet,
            onCopyClick = {
                clipboardManager.setText(AnnotatedString(ethWallet))
            }
        )
        CryptoWalletOption(
            coinName = stringResource(R.string.label_ltc),
            walletAddress = ltcWallet,
            onCopyClick = {
                clipboardManager.setText(AnnotatedString(ltcWallet))
            }
        )
    }
}

@Composable
fun CryptoWalletOption(
    coinName: String,
    walletAddress: String,
    onCopyClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coinName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = walletAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.alpha(0.8f),
                    )
            }
            IconButton (
                onClick = onCopyClick
            ) {
                Icon(
                    modifier = Modifier.alpha(0.8f),
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy wallet address"
                )
            }
        }
    }
}
