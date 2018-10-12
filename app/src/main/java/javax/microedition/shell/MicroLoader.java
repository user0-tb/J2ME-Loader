/*
 * Copyright 2018 Nikita Shakarun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.shell;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.microedition.lcdui.Display;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.midlet.MIDlet;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.FileUtils;

public class MicroLoader {
	private static final String TAG = MicroLoader.class.getName();

	private String path;
	private Context context;

	public MicroLoader(Context context, String path) {
		this.context = context;
		this.path = path;
	}

	public void init() {
		Display.initDisplay();
		Graphics3D.initGraphics3D();
		File cacheDir = ContextHolder.getCacheDir();
		if (cacheDir.exists()) {
			for (File temp : cacheDir.listFiles()) {
				temp.delete();
			}
		}
	}

	public LinkedHashMap<String, String> loadMIDletList() {
		LinkedHashMap<String, String> midlets = new LinkedHashMap<>();
		LinkedHashMap<String, String> params =
				FileUtils.loadManifest(new File(path, Config.MIDLET_MANIFEST_FILE));
		MIDlet.initProps(params);
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getKey().matches("MIDlet-[0-9]+")) {
				String tmp = entry.getValue();
				String key = tmp.substring(tmp.lastIndexOf(',') + 1).trim();
				String value = tmp.substring(0, tmp.indexOf(',')).trim();
				midlets.put(key, value);
			}
		}
		return midlets;
	}

	public MIDlet loadMIDlet(String mainClass)
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		File dexSource = new File(path, Config.MIDLET_DEX_FILE);
		File dexTargetDir = new File(context.getApplicationInfo().dataDir, Config.TEMP_DEX_DIR);
		if (dexTargetDir.exists()) {
			FileUtils.deleteDirectory(dexTargetDir);
		}
		dexTargetDir.mkdir();
		File dexTargetOptDir = new File(context.getApplicationInfo().dataDir, Config.TEMP_DEX_OPT_DIR);
		if (dexTargetOptDir.exists()) {
			FileUtils.deleteDirectory(dexTargetOptDir);
		}
		dexTargetOptDir.mkdir();
		File dexTarget = new File(dexTargetDir, Config.MIDLET_DEX_FILE);
		FileUtils.copyFileUsingChannel(dexSource, dexTarget);
		ClassLoader loader = new MyClassLoader(dexTarget.getAbsolutePath(),
				dexTargetOptDir.getAbsolutePath(), null, context.getClassLoader(),
				path + Config.MIDLET_RES_DIR);
		Log.i(TAG, "loadMIDletList main: " + mainClass + " from dex:" + dexTarget.getPath());
		final MIDlet midlet = (MIDlet) loader.loadClass(mainClass).newInstance();
		return midlet;
	}
}
