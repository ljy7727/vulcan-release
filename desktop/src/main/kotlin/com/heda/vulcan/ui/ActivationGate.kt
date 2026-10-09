package com.heda.vulcan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heda.vulcan.data.AppVersion
import com.heda.vulcan.data.DesktopPrefs
import com.heda.vulcan.data.License

/**
 * 激活门：本版本首次启动（或版本更新后首次启动）时拦截，要求输入序列号。
 * 输入以密码形式遮蔽，校验失败只提示「无效」，不回显任何输入内容。
 */
@Composable
fun ActivationGate(prefs: DesktopPrefs, onActivated: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(420.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("需要激活", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "检测到新版本（v${AppVersion.VERSION}）首次启动，请输入序列号后继续使用。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; error = "" },
                    label = { Text("序列号") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotEmpty()) {
                    Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        if (License.activate(prefs, input)) {
                            onActivated()
                        } else {
                            error = "序列号无效，请重新输入"
                            input = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("激活并使用") }
            }
        }
    }
}
