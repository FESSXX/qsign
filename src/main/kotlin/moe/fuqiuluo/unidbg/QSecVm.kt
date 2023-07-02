@file:OptIn(DelicateCoroutinesApi::class)
package moe.fuqiuluo.unidbg

import com.github.unidbg.linux.android.dvm.DvmObject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.fuqiuluo.comm.UinData
import moe.fuqiuluo.net.SimpleClient
import moe.fuqiuluo.unidbg.env.FileResolver
import moe.fuqiuluo.unidbg.env.QSecJni
import moe.fuqiuluo.unidbg.vm.AndroidVM
import moe.fuqiuluo.unidbg.vm.GlobalData
import org.slf4j.LoggerFactory
import java.io.File
import javax.security.auth.Destroyable
import kotlin.system.exitProcess

class QSecVM(
    val coreLibPath: File,
    uinData: UinData,
    dynarmic: Boolean,
    unicorn: Boolean
): Destroyable, AndroidVM("com.tencent.mobileqq", dynarmic, unicorn) {
    companion object {
        private val logger = LoggerFactory.getLogger(QSecVM::class.java)!!
    }

    private var destroy: Boolean = false
    private var isInit: Boolean = false
    internal val global = GlobalData()
    private val client = SimpleClient("msfwifi.3g.qq.com", 8080)

    init {
        runCatching {
            val resolver = FileResolver(23, this@QSecVM)
            memory.setLibraryResolver(resolver)
            emulator.syscallHandler.addIOResolver(resolver)
            vm.setJni(QSecJni(uinData, this, client, global))
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun init() {
        if (isInit) return
        runCatching {
            GlobalScope.launch {
                client.connect()
                client.initConnection()
            }
            loadLibrary(coreLibPath.resolve("libQSec.so"))
            loadLibrary(coreLibPath.resolve("libfekit.so"))
            this.isInit = true
        }.onFailure {
            it.printStackTrace()
            exitProcess(1)
        }
    }

    fun newInstance(name: String, value: Any? = null, unique: Boolean = false): DvmObject<*> {
        if (unique && name in global) {
            return global[name] as DvmObject<*>
        }
        val obj = findClass(name).newObject(value)
        if (unique) {
            global[name] = obj
        }
        return obj
    }

    override fun isDestroyed(): Boolean = destroy

    override fun destroy() {
        if (isDestroyed) return
        this.destroy = true
        this.client.close()
        this.close()
    }
}