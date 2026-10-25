package io.github.qauxv.integration;

import android.content.Context;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.hook.BaseFunctionHook;
import io.github.qauxv.step.Step;
import io.github.qauxv.util.LicenseStatus;
import io.github.qqmodulemanager.bridge.BridgeClient;
import io.github.qqmodulemanager.bridge.FeatureCatalog;
import java.util.List;
import io.github.qauxv.base.IDynamicHook;
import io.github.qauxv.hook.BaseHookDispatcher;
import io.github.qauxv.core.HookInstaller;
import io.github.qauxv.util.dexkit.DexDeobfsProvider;

public final class QQModuleManagerAdapter {
    private QQModuleManagerAdapter() {}
    public static void register(Context context) {
        BridgeClient.initialize(context,"qauxv",BuildConfig.VERSION_NAME);
        for(FeatureCatalog.Feature f:FeatureCatalog.ALL) {
            List<String> classes=f.classes("qauxv");
            if(!classes.isEmpty())BridgeClient.registerFeature(f.id,()->initialize(classes));
        }
    }
    private static boolean initialize(List<String> classes)throws ReflectiveOperationException {
        for(String name:classes) {
            BaseFunctionHook hook=(BaseFunctionHook)Class.forName(name).getField("INSTANCE").get(null);
            String feature=BridgeClient.featureForClass(name);
            boolean missingDevice=feature.equals("device_model") && (android.text.TextUtils.isEmpty(cc.ioctl.dialog.RikkaCustomDeviceModelDialog.getCurrentDeviceModel())
                    || android.text.TextUtils.isEmpty(cc.ioctl.dialog.RikkaCustomDeviceModelDialog.getCurrentDeviceManufacturer()));
            boolean missingPath=feature.equals("download_redirect") && android.text.TextUtils.isEmpty(
                    io.github.qauxv.config.ConfigManager.getDefaultConfig().getString(io.github.qauxv.config.ConfigItems.qn_file_recv_redirect_path));
            boolean missingSort=feature.equals("emoticon_sort") && cc.microblock.hook.FavEmoticonSortBy.INSTANCE.getState()==cc.microblock.hook.SortOptions.None;
            if(missingDevice || missingPath || missingSort){BridgeClient.configurationRequired(feature);return false;}
            if(hook.isAvailable() && hook.isTargetProcess())return initialize(hook);
        }
        return false;
    }
    private static boolean initialize(BaseFunctionHook hook) {
        if(LicenseStatus.sDisableCommonHooks || !LicenseStatus.hasUserAcceptEula() || !hook.isAvailable() || !hook.isTargetProcess())return false;
        if(!prepareAndInitialize(hook))return false;
        if(hook instanceof com.xiaoniu.dispatcher.OnMenuBuilder && !prepareAndInitialize(com.xiaoniu.dispatcher.MenuBuilderHook.INSTANCE))return false;
        if(hook instanceof me.ketal.dispacher.OnBubbleBuilder && !prepareAndInitialize(me.ketal.dispacher.BaseBubbleBuilderHook.INSTANCE))return false;
        for(IDynamicHook candidate:HookInstaller.queryAllAnnotatedHooks())if(candidate instanceof BaseHookDispatcher) {
            BaseHookDispatcher<?> dispatcher=(BaseHookDispatcher<?>)candidate;
            for(Object decorator:dispatcher.getDecorators())if(decorator==hook && !prepareAndInitialize(dispatcher))return false;
        }
        return true;
    }
    private static boolean prepareAndInitialize(IDynamicHook hook) {
        if(hook.isPreparationRequired()) {
            DexDeobfsProvider.INSTANCE.enterDeobfsSection();
            try {
                Step[] steps=hook.makePreparationSteps();
                if(steps!=null)for(Step step:steps)if(!step.isDone() && !step.step())return false;
            } finally { DexDeobfsProvider.INSTANCE.exitDeobfsSection(); }
        }
        return hook.initialize();
    }
}
