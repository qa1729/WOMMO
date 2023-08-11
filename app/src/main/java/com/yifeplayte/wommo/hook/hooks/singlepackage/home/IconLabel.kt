package com.yifeplayte.wommo.hook.hooks.singlepackage.home

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getBoolean
import com.yifeplayte.wommo.hook.utils.XSharedPreferences.getInt

object IconLabel : BaseHook() {
    override val key = "icon_label"
    override val isEnabled = true
    private val labelSize by lazy { getInt("icon_label_size", 12).toFloat() }
    private val labelMarquee by lazy { getBoolean("icon_label_marquee", false) }

    @SuppressLint("DiscouragedApi")
    override fun hook() {
        val clazzLauncher = loadClass("com.miui.home.launcher.Launcher")
        val clazzShortcutInfo = loadClass("com.miui.home.launcher.ShortcutInfo")
        loadClass("com.miui.home.launcher.ItemIcon").methodFinder().filterByName("onFinishInflate").first().createHook {
            after {
                val mTitle = it.thisObject.objectHelper().getObjectOrNullUntilSuperclassAs<TextView>("mTitle")
                mTitle?.modify()
            }
        }
        loadClass("com.miui.home.launcher.maml.MaMlWidgetView").methodFinder().filterByName("onFinishInflate").first().createHook {
            after {
                val mTitle = it.thisObject.objectHelper().getObjectOrNullUntilSuperclassAs<TextView>("mTitleTextView")
                mTitle?.modify()
            }
        }
        loadClass("com.miui.home.launcher.LauncherMtzGadgetView").methodFinder().filterByName("onFinishInflate").first().createHook {
            after {
                val mTitle = it.thisObject.objectHelper().getObjectOrNullUntilSuperclassAs<TextView>("mTitleTextView")
                mTitle?.modify()
            }
        }
        loadClass("com.miui.home.launcher.LauncherWidgetView").methodFinder().filterByName("onFinishInflate").first().createHook {
            after {
                val mTitle = it.thisObject.objectHelper().getObjectOrNullUntilSuperclassAs<TextView>("mTitleTextView")
                mTitle?.modify()
            }
        }
        loadClass("com.miui.home.launcher.ShortcutIcon").methodFinder().filterByName("fromXml")
            .filterByAssignableParamTypes(Int::class.java, clazzLauncher, ViewGroup::class.java, clazzShortcutInfo).first()
            .createHook {
                after {
                    val buddyIconView = it.args[3].objectHelper().invokeMethodBestMatch("getBuddyIconView", null, it.args[2]) as View
                    val mTitle = buddyIconView.objectHelper().getObjectOrNullUntilSuperclassAs<TextView>("mTitle")
                    mTitle?.modify()
                }
            }
        loadClass("com.miui.home.launcher.common.Utilities").methodFinder().filterByName("adaptTitleStyleToWallpaper").first()
            .createHook {
                after {
                    val mTitle = it.args[1] as TextView
                    if (mTitle.id == mTitle.resources.getIdentifier("icon_title", "id", "com.miui.home")) {
                        mTitle.modify()
                    }
                }
            }
    }

    private fun TextView.modify() {
        textSize = labelSize
        if (labelMarquee) {
            ellipsize = TextUtils.TruncateAt.MARQUEE
            isHorizontalFadingEdgeEnabled = true
            marqueeRepeatLimit = -1
            isSelected = true
            setSingleLine()
            setHorizontallyScrolling(true)
        }
    }
}