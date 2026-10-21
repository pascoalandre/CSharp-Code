package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.csharp.engine.CSharpLexer
import com.example.csharp.engine.CSharpParser
import com.example.csharp.engine.CSharpInterpreter
import com.example.csharp.model.ConsoleEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("C# Studio", appName)
  }

  @Test
  fun `test csharp lexer and parser`() {
    val code = """
        class Program {
            static void Main() {
                int x = 10;
                int y = 20;
                int sum = x + y;
                Console.WriteLine("Soma: " + sum);
            }
        }
    """.trimIndent()
    val parser = CSharpParser(code)
    val program = parser.parseProgram()
    assertTrue(program.classes.isNotEmpty())
    val mainMethod = program.classes[0].methods.find { it.name == "Main" }
    assertNotNull(mainMethod)
    assertEquals(4, mainMethod!!.body.statements.size)
  }

  @Test
  fun `test csharp interpreter execution`() = runTest {
    val code = """
        int a = 5;
        int b = 7;
        int res = a * b;
        Console.WriteLine(res);
    """.trimIndent()
    val parser = CSharpParser(code)
    val program = parser.parseProgram()

    val outputLines = mutableListOf<String>()
    val interpreter = CSharpInterpreter(
        breakpointsProvider = { emptySet() },
        onOutput = { entry -> outputLines.add(entry.text) },
        onStateChange = { _, _, _ -> },
        onConsoleClear = { outputLines.clear() }
    )

    interpreter.execute(program, debugMode = false)
    assertTrue(outputLines.any { it.contains("35") })
  }
}
