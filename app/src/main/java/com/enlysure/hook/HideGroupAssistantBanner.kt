/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package com.enlysure.hook

import android.view.View
import cc.ioctl.util.hookAfterIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object HideGroupAssistantBanner : CommonSwitchFunctionHook() {

    override val name = "隐藏群助手顶部Banner提示"

    override val description = "隐藏群助手顶部 '以下为\"收进群助手且不提醒\"的群消息：' 的提示"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_OTHER

    override fun initOnce(): Boolean {
        if (requireMinQQVersion(QQVersion.QQ_9_3_75)) {
            val clazz = Initiator.loadClass("com.tencent.qqnt.chats.biz.troophelper.c")
            val banner = clazz.getDeclaredField("S").apply { isAccessible = true }
            listOf(
                clazz.getDeclaredMethod("d0", Initiator.loadClass("com.tencent.qqnt.chats.core.ui.ChatsListVB\$b")),
                clazz.getDeclaredMethod("P", Initiator.loadClass("com.tencent.qqnt.chats.core.uistate.b"))
            ).forEach { method ->
                hookAfterIfEnabled(method) { param ->
                    (banner.get(param.thisObject) as? View)?.visibility = View.GONE
                }
            }
            return true
        }
        val createTopBannerMethod = Initiator.loadClass("com/tencent/mobileqq/activity/home/chats/troophelper/TroopHelperFragment")
            .getDeclaredMethod("createTopBanner")
        hookAfterIfEnabled(createTopBannerMethod) { param ->
            (param.result as View).visibility = View.GONE
        }
        return true
    }
}
