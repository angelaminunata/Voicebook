/*
 * Copyright [2015] [name of copyright owner]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smobileteam.voicecall.customview;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ListPreferenceShowSummary extends ListPreference{
	
	public ListPreferenceShowSummary(Context context, AttributeSet attrs) {
		super(context, attrs);
		// init();
	}

	public ListPreferenceShowSummary(Context context) {
		super(context);
		// init();
	}

	@Override
	public CharSequence getSummary() {
		return super.getEntry();
	}

	@Override
	public void setValue(String value) {
		super.setValue(value);
		setSummary(getEntry());
	}

}
