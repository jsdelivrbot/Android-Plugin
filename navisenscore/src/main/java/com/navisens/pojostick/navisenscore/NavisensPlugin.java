package com.navisens.pojostick.navisenscore;

import com.navisens.motiondnaapi.MotionDna;

import java.util.Map;

/**
 * Interface defining the Navisens Base Plugin
 *
 * Created by Joseph Chen on 9/29/17.
 */

@SuppressWarnings("unused")
public interface NavisensPlugin {
    boolean init(com.navisens.pojostick.navisenscore.NavisensCore core, Object[] args);
    boolean stop();
    void receiveMotionDna(MotionDna motionDna);
    void receiveNetworkData(MotionDna motionDna);
    void receiveNetworkData(MotionDna.NetworkCode networkCode, Map<String, ? extends Object> map);
    void receivePluginData(String identifier, Object data);
    void reportError(MotionDna.ErrorCode errorCode, String s);
}
