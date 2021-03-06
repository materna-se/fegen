/**
 * Copyright 2020 Materna Information & Communications SE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.materna.fegen.core.log

import java.util.concurrent.atomic.AtomicBoolean

/**
 * For logging within FeGen.
 * This is an abstraction for the different logging methods of maven and gradle
 */
abstract class FeGenLogger(
        private val context: String,
        protected val errorsEncounteredAtomic: AtomicBoolean
) {

  var errorsEncountered: Boolean
    get() = errorsEncounteredAtomic.get()
    private set(value) = errorsEncounteredAtomic.set(value)

  fun debug(msg: String) {
    printDebug(prependContext(msg))
  }

  protected abstract fun printDebug(msg: String)

  fun info(msg: String) {
    printInfo(prependContext(msg))
  }

  protected abstract fun printInfo(msg: String)

  fun warn(msg: String) {
    printWarn(prependContext(msg))
  }

  abstract fun printWarn(msg: String)

  fun error(msg: String) {
    this.errorsEncountered = true
    printError(prependContext(msg))
  }

  protected abstract fun printError(msg: String)

  abstract fun withContext(context: String): FeGenLogger

  private fun prependContext(msg: String): String {
    return msg.prependIndent("[$context] ")
  }
}