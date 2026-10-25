/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.teble.hook

import android.content.Context
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import java.io.File
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.ForwardSendPicUtil

@FunctionHookEntry
@UiItemAgentEntry
object CancelPicCompress : CommonSwitchFunctionHook(
    SyncUtils.PROC_MAIN or SyncUtils.PROC_PEAK,
    if (requireMinQQVersion(QQVersion.QQ_9_3_75)) emptyArray() else arrayOf(ForwardSendPicUtil)
) {

    override val name = "分享图片不压缩"

    override val description: CharSequence = "通过QQ分享图片时以原图发送"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override val isApplicationRestartRequired = true

    override fun initOnce(): Boolean {
        if (requireMinQQVersion(QQVersion.QQ_9_3_75)) {
            val infoClass = Initiator.loadClass("com.tencent.mobileqq.pic.CompressInfo")
            hookBeforeIfEnabled(Initiator.loadClass("com.tencent.mobileqq.pic.api.impl.CompressOperatorImpl")
                .getDeclaredMethod("start", infoClass)) { param ->
                if (Thread.currentThread().stackTrace.none { it.className == "com.tencent.mobileqq.utils.ForwardSendPicUtil" }) {
                    return@hookBeforeIfEnabled
                }
                val info = param.args[0]
                infoClass.getField("s").set(info, infoClass.getField("o").get(info))
                infoClass.getField("f").setBoolean(info, true)
                infoClass.getField("A").setBoolean(info, true)
                param.result = true
            }
            hookBeforeIfEnabled(Initiator.loadClass("com.tencent.mobileqq.utils.ba").getDeclaredMethod("c",
                Int::class.java, Context::class.java, String::class.java, String::class.java,
                Boolean::class.java, Initiator.loadClass("com.tencent.mobileqq.activity.photo.ImageInfo"), Int::class.java)) { param ->
                if (Thread.currentThread().stackTrace.none { it.className == "com.tencent.mobileqq.utils.ForwardSendPicUtil" }) {
                    return@hookBeforeIfEnabled
                }
                val source = File(param.args[2] as String)
                val destination = File(param.args[3] as String)
                if (source.canonicalFile != destination.canonicalFile) {
                    source.copyTo(destination, overwrite = true)
                }
                param.result = true
            }
            return true
        }
        val util = DexKit.loadClassFromCache(ForwardSendPicUtil)!!
        findMethod(util) {
            (parameterTypes[0] == Context::class.java) and (parameterTypes[1] == String::class.java)
        }.hookReplace { param ->
            param.args[1] as String?
        }
        return true
    }
}
