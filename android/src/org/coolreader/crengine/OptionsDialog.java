package org.coolreader.crengine;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.crengine.ColorPickerDialog.OnColorChangedListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

public class OptionsDialog extends BaseDialog implements TabContentFactory {

	ReaderView mReaderView;
	CoolReader mActivity;
	String[] mFontFaces;
	int[] mFontSizes = new int[] {
		14, 16, 18, 20, 22, 24, 26, 28, 30,
		32, 34, 36, 38, 40, 42, 44, 48, 52, 56, 60, 64, 68
	};
	int[] mStatusFontSizes = new int[] {
			14, 16, 18, 20, 22, 24, 26, 28, 30,
			32
		};
	int[] mBacklightLevels = new int[] {
		-1, 1, 5, 10, 15, 20, 30, 40, 50, 60, 70, 80, 100
	};
	String[] mBacklightLevelsTitles = new String[] {
			"Default", "1%", "5%", "10%", "15%", "20%", "30%", "40%", "50%", "60%", "70%", "80%", "100%",
	};
	int[] mInterlineSpaces = new int[] {
			80, 90, 100, 110, 120, 130, 140, 150
		};
	int[] mMargins = new int[] {
			0, 1, 2, 3, 4, 5, 10, 15, 20, 25
		};
	int[] mOrientations = new int[] {
			0, 1//, 2, 3
			,4
		};
	int[] mOrientationsTitles = new int[] {
			R.string.options_page_orientation_0, R.string.options_page_orientation_90 //, R.string.options_page_orientation_180, R.string.options_page_orientation_270
			,R.string.options_page_orientation_sensor
		};
	int[] mAnimation = new int[] {
			ReaderView.PAGE_ANIMATION_NONE, ReaderView.PAGE_ANIMATION_SLIDE, 
			ReaderView.PAGE_ANIMATION_PAPER
		};
	int[] mAnimationTitles = new int[] {
			R.string.options_page_animation_none, R.string.options_page_animation_slide,
			R.string.options_page_animation_paperbook
		};
	int[] mAntialias = new int[] {
			0, 1, 2
		};
	int[] mAntialiasTitles = new int[] {
			R.string.options_font_antialias_off, R.string.options_font_antialias_on_for_big, R.string.options_font_antialias_on_for_all
		};
	int[] mLandscapePages = new int[] {
			1, 2
		};
	int[] mLandscapePagesTitles = new int[] {
			R.string.options_page_landscape_pages_one, R.string.options_page_landscape_pages_two
		};
	int[] mViewModes = new int[] {
			1, 0
		};
	int[] mViewModeTitles = new int[] {
			R.string.options_view_mode_pages, R.string.options_view_mode_scroll
		};
	TabHost mTabs;
	LayoutInflater mInflater;
	Properties mProperties;
	Properties mOldProperties;
	OptionsListView mOptionsStyles;
	OptionsListView mOptionsPage;
	OptionsListView mOptionsApplication;
	OptionsListView mOptionsControls;

	private final int OPTION_VIEW_TYPE_NORMAL = 0;
	private final int OPTION_VIEW_TYPE_BOOLEAN = 1;
	private final int OPTION_VIEW_TYPE_COLOR = 2;
	private final int OPTION_VIEW_TYPE_COUNT = 3;
	
	class OptionBase {
		protected View myView;
		public String label;
		public String property;
		public String defaultValue;
		public int iconId = R.drawable.cr3_option_other;
		public OptionsListView optionsListView;
		protected Runnable onChangeHandler;
		public OptionBase( String label, String property ) {
			this.label = label;
			this.property = property;
		}
		public OptionBase setIconId(int id) {
			this.iconId = id;
			return this;
		}
		public OptionBase setDefaultValue(String value) {
			this.defaultValue = value;
			if ( mProperties.getProperty(property)==null )
				mProperties.setProperty(property, value);
			return this;
		}
		public void setOnChangeHandler( Runnable handler ) {
			onChangeHandler = handler;
		}

		public int getItemViewType() {
			return OPTION_VIEW_TYPE_NORMAL;
		}

		protected void refreshItem()
		{
			getView(null, null).invalidate();
			//if ( optionsListView!=null )
			//	optionsListView.refresh();
		}

		protected void refreshList()
		{
			getView(null, null).invalidate();
			if ( optionsListView!=null )
				optionsListView.refresh();
		}

		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				//view = new TextView(getContext());
				view = mInflater.inflate(R.layout.option_item, null);
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			TextView valueView = (TextView)view.findViewById(R.id.option_value);
			labelView.setText(label);
			valueView.setText(getValueLabel());
			ImageView icon = (ImageView)view.findViewById(R.id.option_icon);
			icon.setImageResource(iconId);
			return view;
		}

		public String getValueLabel() { return mProperties.getProperty(property); }
		public void onSelect() { refreshList(); }
	}
	
	class ColorOption extends OptionBase {
		final int defColor;
		public ColorOption( String label, String property, int defColor ) {
			super(label, property);
			this.defColor = defColor;
		}
		public String getValueLabel() { return mProperties.getProperty(property); }
		public void onSelect()
		{ 
			ColorPickerDialog dlg = new ColorPickerDialog(mActivity, new OnColorChangedListener() {
				public void colorChanged(int color) {
					mProperties.setColor(property, color);
					if ( property.equals(ReaderView.PROP_BACKGROUND_COLOR) ) {
						String texture = mProperties.getProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE, Engine.NO_TEXTURE.id);
						if ( texture!=null && !texture.equals(Engine.NO_TEXTURE.id) ) {
							// reset background image
							mProperties.setProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE, Engine.NO_TEXTURE.id);
							// TODO: show notification?
						}
					}
					refreshList();
				}
			}, mProperties.getColor(property, defColor), label);
			dlg.show();
		}
		public int getItemViewType() {
			return OPTION_VIEW_TYPE_COLOR;
		}
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				//view = new TextView(getContext());
				view = mInflater.inflate(R.layout.option_item_color, null);
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			ImageView valueView = (ImageView)view.findViewById(R.id.option_value_color);
			labelView.setText(label);
			int cl = mProperties.getColor(property, defColor);
			valueView.setBackgroundColor(cl);
			ImageView icon = (ImageView)view.findViewById(R.id.option_icon);
			icon.setImageResource(iconId);
			return view;
		}
	}
	
	class BoolOption extends OptionBase {
		private boolean inverse = false;
		public BoolOption( String label, String property ) {
			super(label, property);
		}
		private boolean getValueBoolean() { return "1".equals(mProperties.getProperty(property)) ^ inverse; }
		public String getValueLabel() { return getValueBoolean()  ? getString(R.string.options_value_on) : getString(R.string.options_value_off); }
		public void onSelect() { 
			mProperties.setProperty(property, "1".equals(mProperties.getProperty(property)) ? "0" : "1");
			refreshList();
		}
		public BoolOption setInverse() { inverse = true; return this; }
		public int getItemViewType() {
			return OPTION_VIEW_TYPE_BOOLEAN;
		}
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				//view = new TextView(getContext());
				view = mInflater.inflate(R.layout.option_item_boolean, null);
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			CheckBox valueView = (CheckBox)view.findViewById(R.id.option_value_cb);
//			valueView.setFocusable(false);
//			valueView.setClickable(false);
			labelView.setText(label);
			valueView.setChecked(getValueBoolean());
			valueView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton arg0,
							boolean checked) {
//						mProperties.setBool(property, checked);
//						refreshList();
					}
				});
			ImageView icon = (ImageView)view.findViewById(R.id.option_icon);
			icon.setImageResource(iconId);
//			view.setClickable(true);
//			view.setFocusable(true);
			return view;
		}
	}

	static public void saveColor( Properties mProperties, boolean night )
	{
		if ( night ) {
			mProperties.setProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE_NIGHT, mProperties.getProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE, "(NONE)"));
			mProperties.setColor(ReaderView.PROP_BACKGROUND_COLOR_NIGHT, mProperties.getColor(ReaderView.PROP_BACKGROUND_COLOR, 0x000000));
			mProperties.setColor(ReaderView.PROP_FONT_COLOR_NIGHT, mProperties.getColor(ReaderView.PROP_FONT_COLOR, 0xFFFFFF));
			mProperties.setColor(ReaderView.PROP_STATUS_FONT_COLOR_NIGHT, mProperties.getColor(ReaderView.PROP_STATUS_FONT_COLOR, 0xFFFFFF));
			mProperties.setInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT_NIGHT, mProperties.getInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT, -1));
		} else {
			mProperties.setProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE_DAY, mProperties.getProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE, "(NONE)"));
			mProperties.setColor(ReaderView.PROP_BACKGROUND_COLOR_DAY, mProperties.getColor(ReaderView.PROP_BACKGROUND_COLOR, 0xFFFFFF));
			mProperties.setColor(ReaderView.PROP_FONT_COLOR_DAY, mProperties.getColor(ReaderView.PROP_FONT_COLOR, 0x000000));
			mProperties.setColor(ReaderView.PROP_STATUS_FONT_COLOR_DAY, mProperties.getColor(ReaderView.PROP_STATUS_FONT_COLOR, 0x000000));
			mProperties.setInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT_DAY, mProperties.getInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT, -1));
		}
	}
	static public void restoreColor( Properties mProperties,  boolean night )
	{
		if ( night ) {
			mProperties.setProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE, mProperties.getProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE_NIGHT, "(NONE)"));
			mProperties.setColor(ReaderView.PROP_BACKGROUND_COLOR, mProperties.getColor(ReaderView.PROP_BACKGROUND_COLOR_NIGHT, 0x000000));
			mProperties.setColor(ReaderView.PROP_FONT_COLOR, mProperties.getColor(ReaderView.PROP_FONT_COLOR_NIGHT, 0xFFFFFF));
			mProperties.setColor(ReaderView.PROP_STATUS_FONT_COLOR, mProperties.getColor(ReaderView.PROP_STATUS_FONT_COLOR_NIGHT, 0xFFFFFF));
			mProperties.setInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT, mProperties.getInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT_NIGHT, 70));
		} else {
			mProperties.setProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE, mProperties.getProperty(ReaderView.PROP_PAGE_BACKGROUND_IMAGE_DAY, "(NONE)"));
			mProperties.setColor(ReaderView.PROP_BACKGROUND_COLOR, mProperties.getColor(ReaderView.PROP_BACKGROUND_COLOR_DAY, 0xFFFFFF));
			mProperties.setColor(ReaderView.PROP_FONT_COLOR, mProperties.getColor(ReaderView.PROP_FONT_COLOR_DAY, 0x000000));
			mProperties.setColor(ReaderView.PROP_STATUS_FONT_COLOR, mProperties.getColor(ReaderView.PROP_STATUS_FONT_COLOR_DAY, 0x000000));
			mProperties.setInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT, mProperties.getInt(ReaderView.PROP_APP_SCREEN_BACKLIGHT_DAY, 80));
		}
	}

	static public void toggleDayNightMode( Properties mProperties ) {
		boolean oldMode = mProperties.getBool(ReaderView.PROP_NIGHT_MODE, false);
		saveColor(mProperties, oldMode);
		boolean newMode = !oldMode;
		restoreColor(mProperties, newMode);
		mProperties.setBool(ReaderView.PROP_NIGHT_MODE, newMode);
	}

	class NightModeOption extends BoolOption {
		public NightModeOption( String label, String property ) {
			super(label, property);
		}
		public void onSelect() { 
			toggleDayNightMode(mProperties);
			refreshList();
		}
	}
	
	class ActionOption extends ListOption {
		public ActionOption( String label, String property, boolean isTap, boolean allowRepeat ) {
			super(label, property);
			ReaderAction[] actions = ReaderAction.AVAILABLE_ACTIONS;
			for ( ReaderAction a : actions )
				if ( !isTap || a.mayAssignOnTap() )
					add(a.id, getString(a.nameId));
			if ( allowRepeat )
				add(ReaderAction.REPEAT.id, getString(ReaderAction.REPEAT.nameId));
			if ( mProperties.getProperty(property)==null )
				mProperties.setProperty(property, ReaderAction.NONE.id);
		}
	}

	class KeyMapOption extends ListOption {
		public KeyMapOption( String label ) {
			super(label, ReaderView.PROP_APP_KEY_ACTIONS_PRESS);
		}
		private void addKey( OptionsListView list, int keyCode, String keyName ) {
			final String propName = ReaderAction.getKeyProp(keyCode, ReaderAction.NORMAL);
			final String longPropName = ReaderAction.getKeyProp(keyCode, ReaderAction.LONG);
			final String dblPropName = ReaderAction.getKeyProp(keyCode, ReaderAction.DOUBLE);
			list.add(new ActionOption(keyName, propName, false, false));
			list.add(new ActionOption(keyName + " " + getContext().getString(R.string.options_app_key_long_press), longPropName, false, true));
			list.add(new ActionOption(keyName + " " + getContext().getString(R.string.options_app_key_double_press), dblPropName, false, false));
		}
		public void onSelect() {
			BaseDialog dlg = new BaseDialog(mActivity, R.string.dlg_button_ok, 0, false);
			OptionsListView listView = new OptionsListView(getContext());
			addKey(listView, KeyEvent.KEYCODE_DPAD_LEFT, "Left");
			addKey(listView, KeyEvent.KEYCODE_DPAD_RIGHT, "Right");
			addKey(listView, KeyEvent.KEYCODE_DPAD_UP, "Up");
			addKey(listView, KeyEvent.KEYCODE_DPAD_DOWN, "Down");
			addKey(listView, KeyEvent.KEYCODE_DPAD_CENTER, "Center");
			addKey(listView, KeyEvent.KEYCODE_BACK, "Back");
			addKey(listView, KeyEvent.KEYCODE_MENU, "Menu");
			addKey(listView, KeyEvent.KEYCODE_SEARCH, "Search");
			addKey(listView, KeyEvent.KEYCODE_VOLUME_UP, "Volume Up");
			addKey(listView, KeyEvent.KEYCODE_VOLUME_DOWN, "Volume Down");
			addKey(listView, KeyEvent.KEYCODE_CAMERA, "Camera");
			dlg.setTitle(label);
			dlg.setView(listView);
			dlg.show();
		}

		public String getValueLabel() { return ">"; }
	}
	
	class StatusBarOption extends ListOption {
		public StatusBarOption( String label ) {
			super(label, ReaderView.PROP_SHOW_TITLE);
		}
		public void onSelect() {
			BaseDialog dlg = new BaseDialog(mActivity, R.string.dlg_button_ok, 0, false);
			OptionsListView listView = new OptionsListView(getContext());
			listView.add(new BoolOption(getString(R.string.options_page_show_titlebar), ReaderView.PROP_STATUS_LINE).setInverse().setDefaultValue("0"));
			listView.add(new ListOption(getString(R.string.options_page_titlebar_font_face), ReaderView.PROP_STATUS_FONT_FACE).add(mFontFaces).setDefaultValue(mFontFaces[0]).setIconId(R.drawable.cr3_option_font_face));
			listView.add(new ListOption(getString(R.string.options_page_titlebar_font_size), ReaderView.PROP_STATUS_FONT_SIZE).add(mStatusFontSizes).setDefaultValue("18").setIconId(R.drawable.cr3_option_font_size));
			listView.add(new ColorOption(getString(R.string.options_page_titlebar_font_color), ReaderView.PROP_STATUS_FONT_COLOR, 0x000000));
			listView.add(new BoolOption(getString(R.string.options_page_show_titlebar_title), ReaderView.PROP_SHOW_TITLE).setDefaultValue("1"));
			listView.add(new BoolOption(getString(R.string.options_page_show_titlebar_chapter_marks), ReaderView.PROP_STATUS_CHAPTER_MARKS).setDefaultValue("1"));
			listView.add(new BoolOption(getString(R.string.options_page_show_titlebar_battery_percent), ReaderView.PROP_SHOW_BATTERY_PERCENT).setDefaultValue("1"));
			dlg.setTitle(label);
			dlg.setView(listView);
			dlg.show();
		}

		public String getValueLabel() { return ">"; }
	}
	
	class TapZoneOption extends OptionBase {
		public TapZoneOption( String label, String property ) {
			super(label, property);
		}
		View grid;
		private void initTapZone( View view, final int tapZoneId )
		{
			if ( view==null )
				return;
			final TextView text = (TextView)view.findViewById(R.id.tap_zone_action_text_short);
			final TextView longtext = (TextView)view.findViewById(R.id.tap_zone_action_text_long);
			final String propName = property + "." + tapZoneId;
			final String longPropName = property + ".long." + tapZoneId;
			ReaderAction action = ReaderAction.findById( mProperties.getProperty(propName) );
			ReaderAction longAction = ReaderAction.findById( mProperties.getProperty(longPropName) );
			text.setText(getString(action.nameId));
			longtext.setText(getString(longAction.nameId));
			view.setLongClickable(true);
			view.setOnClickListener(new View.OnClickListener () {
				public void onClick(View v) {
					// TODO: i18n
					ActionOption option = new ActionOption(getString(R.string.options_app_tap_action_short), propName, true, false);
					option.setOnChangeHandler(new Runnable() {
						public void run() {
							ReaderAction action = ReaderAction.findById( mProperties.getProperty(propName) );
							text.setText(getString(action.nameId));
						}
					});
					option.onSelect();
				}
			});
			view.setOnLongClickListener(new View.OnLongClickListener () {
				public boolean onLongClick(View v) {
					// TODO: i18n
					ActionOption option = new ActionOption(getString(R.string.options_app_tap_action_long), longPropName, true, true);
					option.setOnChangeHandler(new Runnable() {
						public void run() {
							ReaderAction longAction = ReaderAction.findById( mProperties.getProperty(longPropName) );
							longtext.setText(getString(longAction.nameId));
						}
					});
					option.onSelect();
					return true;
				}
			});
		}

		public String getValueLabel() { return ">"; }
		public void onSelect() {
			BaseDialog dlg = new BaseDialog(mActivity, R.string.dlg_button_ok, 0, false);
			grid = (View)mInflater.inflate(R.layout.options_tap_zone_grid, null);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell1), 1);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell2), 2);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell3), 3);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell4), 4);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell5), 5);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell6), 6);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell7), 7);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell8), 8);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell9), 9);
			dlg.setTitle(label);
			dlg.setView(grid);
			dlg.show();
		}
	}
	
	class Pair {
		public String value;
		public String label;
		public Pair(String value, String label) {
			this.value = value;
			this.label = label;
		}
	}

	class ListOption extends OptionBase {
		private ArrayList<Pair> list = new ArrayList<Pair>();
		public ListOption( String label, String property ) {
			super(label, property);
		}
		public void add(String value, String label) {
			list.add( new Pair(value, label) );
		}
		public ListOption add(String[]values) {
			for ( String item : values ) {
				add(item, item);
			}
			return this;
		}
		public ListOption add(int[]values) {
			for ( int item : values ) {
				String s = String.valueOf(item); 
				add(s, s);
			}
			return this;
		}
		public ListOption add(int[]values, int[]labelIDs) {
			for ( int i=0; i<values.length; i++ ) {
				String value = String.valueOf(values[i]); 
				String label = getContext().getString(labelIDs[i]); 
				add(value, label);
			}
			return this;
		}
		public ListOption add(int[]values, String[]labels) {
			for ( int i=0; i<values.length; i++ ) {
				String value = String.valueOf(values[i]); 
				String label = labels[i]; 
				add(value, label);
			}
			return this;
		}
		public ListOption addPercents(int[]values) {
			for ( int item : values ) {
				String s = String.valueOf(item); 
				add(s, s + "%");
			}
			return this;
		}
		public String findValueLabel( String value ) {
			for ( Pair pair : list ) {
				if ( value!=null && pair.value.equals(value) )
					return pair.label;
			}
			return null;
		}
		public int findValue( String value ) {
			if ( value==null )
				return -1;
			for ( int i=0; i<list.size(); i++ ) {
				if ( value.equals(list.get(i).value) )
					return i;
			}
			return -1;
		}
		
		public int getSelectedItemIndex() {
			return findValue(mProperties.getProperty(property));
		}

		protected void closed() {
			
		}
		
		protected int getItemLayoutId() {
			return R.layout.option_value; 
		}
		
		protected void updateItemContents( final View layout, final Pair item, final ListView listView, final int position ) {
			TextView view;
			RadioButton cb;
			view = (TextView)layout.findViewById(R.id.option_value_text);
			cb = (RadioButton)layout.findViewById(R.id.option_value_check);
			view.setText(item.label);
			String currValue = mProperties.getProperty(property);
			boolean isSelected = item.value!=null && currValue!=null && item.value.equals(currValue) ;//getSelectedItemIndex()==position;
			cb.setChecked(isSelected);
			cb.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					listView.getOnItemClickListener().onItemClick(listView, listView, position, 0);
//					mProperties.setProperty(property, item.value);
//					dismiss();
//					optionsListView.refresh();
				}
			});
		}
		
		public String getValueLabel() { return findValueLabel(mProperties.getProperty(property)); }
		
		public void onSelect() {
			final BaseDialog dlg = new BaseDialog(mActivity, 0, 0, false);
			//AlertDialog.Builder dlg = new AlertDialog.Builder(getContext());
			dlg.setTitle(label);

			final ListView listView = new ListView(getContext());
			
			
			ListAdapter listAdapter = new ListAdapter() {

				public boolean areAllItemsEnabled() {
					return true;
				}

				public boolean isEnabled(int position) {
					return true;
				}

				public int getCount() {
					return list.size();
				}

				public Object getItem(int position) {
					return list.get(position);
				}

				public long getItemId(int position) {
					return position;
				}

				public int getItemViewType(int position) {
					return 0;
				}

				public View getView(final int position, View convertView,
						ViewGroup parent) {
					ViewGroup layout;
					if ( convertView==null ) {
						layout = (ViewGroup)mInflater.inflate(getItemLayoutId(), null);
						//view = new TextView(getContext());
					} else {
						layout = (ViewGroup)convertView;
					}
					final Pair item = list.get(position);
					updateItemContents( layout, item, listView, position );
					//cb.setClickable(false);
//					cb.setOnClickListener(new View.OnClickListener() {
//						@Override
//						public void onClick(View v) {
//							
//						}
//					});
					return layout;
				}

				public int getViewTypeCount() {
					return 1;
				}

				public boolean hasStableIds() {
					return true;
				}

				public boolean isEmpty() {
					return list.size()==0;
				}

				private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();
				
				public void registerDataSetObserver(DataSetObserver observer) {
					observers.add(observer);
				}

				public void unregisterDataSetObserver(DataSetObserver observer) {
					observers.remove(observer);
				}
				
			};
			int selItem = getSelectedItemIndex();
			if ( selItem<0 )
				selItem = 0;
			listView.setAdapter(listAdapter);
			listView.setSelection(selItem);
			dlg.setView(listView);
			//final AlertDialog d = dlg.create();
			listView.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> adapter, View listview,
						int position, long id) {
					Pair item = list.get(position);
					onClick(item);
					dlg.dismiss();
					closed();
				}
			});
			dlg.show();
		}
		
		public void onClick( Pair item ) {
			mProperties.setProperty(property, item.value);
			refreshList();
			if ( onChangeHandler!=null )
				onChangeHandler.run();
			if ( optionsListView!=null )
				optionsListView.refresh();
		}
	}
	
	class HyphenationOptions extends ListOption
	{
		public HyphenationOptions( String label )
		{
			super( label, ReaderView.PROP_HYPHENATION_DICT );
			setDefaultValue("RUSSIAN");
			Engine.HyphDict[] dicts = Engine.HyphDict.values();
			for ( Engine.HyphDict dict : dicts )
				add( dict.toString(), dict.name );
		}
	}
	
	class ThumbnailCache {
		final int maxcount;
		final int dx;
		final int dy;
		class Item {
			Drawable drawable;
			Bitmap bmp;
			String path;
			int id;
			public void clear() {
				if ( bmp!=null ) {
					bmp.recycle();
					bmp = null;
				}
				if ( drawable!=null )
					drawable = null;
			}
		}
		ArrayList<Item> list = new ArrayList<Item>(); 
		public ThumbnailCache( int dx, int dy, int maxcount ) {
			this.dx = dx;
			this.dy = dy;
			this.maxcount = maxcount;
		}
		private void remove( int maxsize ) {
			while ( list.size()>maxsize ) {
				Item item = list.remove(0);
				item.clear();
			}
		}
		private Drawable createDrawable( String path ) {
			File f = new File(path);
			if ( !f.isFile() || !f.exists() )
				return null;
			try { 
				BitmapDrawable drawable = (BitmapDrawable)BitmapDrawable.createFromPath(path);
				if ( drawable==null )
					return null;
				Bitmap src = drawable.getBitmap();
				Bitmap bmp = Bitmap.createScaledBitmap(src, dx, dy, true);
				//Canvas canvas = new Canvas(bmp);
				BitmapDrawable res = new BitmapDrawable(bmp);
				//src.recycle();
				Item item = new Item();
				item.path = path;
				item.drawable = res; //drawable;
				item.bmp = bmp;
				list.add(item);
				remove(maxcount);
				return drawable;
			} catch ( Exception e ) {
				return null;
			}
		}
		private Drawable createDrawable( int resourceId ) {
			try { 
				//Drawable drawable = mReaderView.getActivity().getResources().getDrawable(resourceId);
				InputStream is = getContext().getResources().openRawResource(resourceId);
				if ( is==null )
					return null;
				BitmapDrawable src = new BitmapDrawable(is);
				if ( src==null )
					return null;
				Item item = new Item();
				item.id = resourceId;
				Bitmap bmp = Bitmap.createScaledBitmap(src.getBitmap(), dx, dy, true);
				BitmapDrawable res = new BitmapDrawable(bmp);
				item.drawable = res;
				item.bmp = bmp;
				list.add(item);
				remove(maxcount);
				return res;
			} catch ( Exception e ) {
				return null;
			}
		}
		public Drawable getImage( String path ) {
			if ( path==null || !path.startsWith("/"))
				return null;
			// find existing
			for ( int i=0; i<list.size(); i++ ) {
				if ( list.get(i).path!=null && path.equals(list.get(i).path) ) {
					Item item = list.remove(i);
					list.add(item);
					return item.drawable;
				}
			}
			return createDrawable( path ); 
		}
		public Drawable getImage( int resourceId ) {
			if ( resourceId==0 )
				return null;
			// find existing
			for ( int i=0; i<list.size(); i++ ) {
				if ( list.get(i).id == resourceId ) {
					Item item = list.remove(i);
					list.add(item);
					return item.drawable;
				}
			}
			return createDrawable( resourceId ); 
		}
		public void clear() {
			remove(0);
		}
	}
	
	ThumbnailCache textureSampleCache = new ThumbnailCache(64, 64, 100);
	
	class TextureOptions extends ListOption
	{
		public TextureOptions( String label )
		{
			super( label, ReaderView.PROP_PAGE_BACKGROUND_IMAGE );
			setDefaultValue("(NONE)");
			BackgroundTextureInfo[] textures = mReaderView.getEngine().getAvailableTextures();
			for ( BackgroundTextureInfo item : textures )
				add( item.id, item.name );
		}

		protected void closed() {
			textureSampleCache.clear();
		}

		protected int getItemLayoutId() {
			return R.layout.option_value_image; 
		}
		
		protected void updateItemContents( final View layout, final Pair item, final ListView listView, final int position ) {
			super.updateItemContents(layout, item, listView, position);
			ImageView img = (ImageView)layout.findViewById(R.id.option_value_image);
			int cl = mProperties.getColor(ReaderView.PROP_BACKGROUND_COLOR, Color.WHITE);
			BackgroundTextureInfo texture = mReaderView.getEngine().getTextureInfoById(item.value);
			img.setBackgroundColor(cl);
			if ( texture.resourceId!=0 ) {
//				img.setImageDrawable(null);
//				img.setImageResource(texture.resourceId);
//				img.setBackgroundColor(Color.TRANSPARENT);
				Drawable drawable = textureSampleCache.getImage(texture.resourceId);
				if ( drawable!=null ) {
					img.setImageResource(0);
					img.setImageDrawable(drawable);
					img.setBackgroundColor(Color.TRANSPARENT);
				} else {
					img.setBackgroundColor(cl);
					img.setImageResource(0);
					img.setImageDrawable(null);
				}
			} else {
				// load image from file
				Drawable drawable = textureSampleCache.getImage(texture.id);
				if ( drawable!=null ) {
					img.setImageResource(0);
					img.setImageDrawable(drawable);
					img.setBackgroundColor(Color.TRANSPARENT);
				} else {
					img.setBackgroundColor(cl);
					img.setImageResource(0);
					img.setImageDrawable(null);
				}
			}
		}
	}
	
	//byte[] fakeLongArrayForDebug;
	
	public OptionsDialog( CoolReader activity, ReaderView readerView, String[] fontFaces )
	{
		super(activity, R.string.dlg_button_ok, R.string.dlg_button_cancel, false);
		
		mActivity = activity;
		mReaderView = readerView;
		mFontFaces = fontFaces;
		mProperties = readerView.getSettings();

		//fakeLongArrayForDebug = new byte[2000000]; // 2M
		//CoolReader.dumpHeapAllocation();
	}
	
	class OptionsListView extends ListView {
		private ArrayList<OptionBase> mOptions = new ArrayList<OptionBase>();
		private ListAdapter mAdapter;
		public void refresh()
		{
			//setAdapter(mAdapter);
			for ( OptionBase item : mOptions ) {
				item.refreshItem();
			}
			invalidate();
		}
		public OptionsListView add( OptionBase option ) {
			mOptions.add(option);
			option.optionsListView = this;
			return this;
		}
		public OptionsListView( Context context )
		{
			super(context);
			setFocusable(true);
			setFocusableInTouchMode(true);
			mAdapter = new ListAdapter() {
				public boolean areAllItemsEnabled() {
					return false;
				}

				public boolean isEnabled(int position) {
					boolean isPageMode = mProperties.getBool(ReaderView.PROP_PAGE_VIEW_MODE, true);
					OptionBase option = mOptions.get(position);
					String prop = option.property;
					if ( prop.equals(ReaderView.PROP_STATUS_LINE) || prop.equals(ReaderView.PROP_FOOTNOTES) )
						return isPageMode;
					return true;
				}

				public int getCount() {
					return mOptions.size();
				}

				public Object getItem(int position) {
					return mOptions.get(position);
				}

				public long getItemId(int position) {
					return position;
				}

				public int getItemViewType(int position) {
//					OptionBase item = mOptions.get(position);
//					return item.getItemViewType();
					return position;
				}

				
				public View getView(int position, View convertView, ViewGroup parent) {
					OptionBase item = mOptions.get(position);
					return item.getView(convertView, parent);
				}

				public int getViewTypeCount() {
					//return OPTION_VIEW_TYPE_COUNT;
					return mOptions.size() > 0 ? mOptions.size() : 1;
				}

				public boolean hasStableIds() {
					return true;
				}

				public boolean isEmpty() {
					return mOptions.size()==0;
				}

				private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();
				
				public void registerDataSetObserver(DataSetObserver observer) {
					observers.add(observer);
				}

				public void unregisterDataSetObserver(DataSetObserver observer) {
					observers.remove(observer);
				}
			};
			setAdapter(mAdapter);
		}
		@Override
		public boolean performItemClick(View view, int position, long id) {
			mOptions.get(position).onSelect();
			return true;
		}
		
	}
	
	public View createTabContent(String tag) {
		if ( "App".equals(tag) )
			return mOptionsApplication;
		else if ( "Styles".equals(tag) )
			return mOptionsStyles;
		else if ( "Controls".equals(tag) )
			return mOptionsControls;
		else if ( "Page".equals(tag))
			return mOptionsPage;
		return null;
	}

	private String getString( int resourceId )
	{
		return getContext().getResources().getString(resourceId); 
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating OptionsDialog");
		CoolReader.dumpHeapAllocation();
		Log.v("cr3", "calling gc");
		System.gc();
		CoolReader.dumpHeapAllocation();
		Log.v("cr3", "creating options dialog");
		setTitle(null);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        mInflater = LayoutInflater.from(getContext());
        mTabs = (TabHost)mInflater.inflate(R.layout.options, null);
		// setup tabs
		//setView(R.layout.options);
		//setContentView(R.layout.options);
		//mTabs = (TabHost)findViewById(android.R.id.tabhost); 
		mTabs.setup();
		//new TabHost(getContext());
		mOptionsStyles = new OptionsListView(getContext());
		mOptionsStyles.add(new ListOption(getString(R.string.options_font_face), ReaderView.PROP_FONT_FACE).add(mFontFaces).setDefaultValue(mFontFaces[0]).setIconId(R.drawable.cr3_option_font_face));
		mOptionsStyles.add(new ListOption(getString(R.string.options_font_size), ReaderView.PROP_FONT_SIZE).add(mFontSizes).setDefaultValue("24").setIconId(R.drawable.cr3_option_font_size));
		mOptionsStyles.add(new BoolOption(getString(R.string.options_font_embolden), ReaderView.PROP_FONT_WEIGHT_EMBOLDEN).setDefaultValue("0").setIconId(R.drawable.cr3_option_text_bold));
		//mOptionsStyles.add(new BoolOption(getString(R.string.options_font_antialias), ReaderView.PROP_FONT_ANTIALIASING).setInverse().setDefaultValue("0"));
		mOptionsStyles.add(new ListOption(getString(R.string.options_font_antialias), ReaderView.PROP_FONT_ANTIALIASING).add(mAntialias, mAntialiasTitles).setDefaultValue("2"));
		mOptionsStyles.add(new ListOption(getString(R.string.options_interline_space), ReaderView.PROP_INTERLINE_SPACE).addPercents(mInterlineSpaces).setDefaultValue("100"));
		mOptionsStyles.add(new NightModeOption(getString(R.string.options_inverse_view), ReaderView.PROP_NIGHT_MODE));
		mOptionsStyles.add(new ColorOption(getString(R.string.options_color_text), ReaderView.PROP_FONT_COLOR, 0x000000));
		mOptionsStyles.add(new ColorOption(getString(R.string.options_color_background), ReaderView.PROP_BACKGROUND_COLOR, 0xFFFFFF));
		mOptionsStyles.add(new TextureOptions(getString(R.string.options_background_texture)));

		mBacklightLevelsTitles[0] = getString(R.string.options_app_backlight_screen_default);
		mOptionsStyles.add(new ListOption(getString(R.string.options_app_backlight_screen), ReaderView.PROP_APP_SCREEN_BACKLIGHT).add(mBacklightLevels, mBacklightLevelsTitles).setDefaultValue("-1"));
		//
		mOptionsStyles.add(new HyphenationOptions(getString(R.string.options_hyphenation_dictionary)));
		//
		mOptionsPage = new OptionsListView(getContext());
		mOptionsPage.add(new ListOption(getString(R.string.options_view_mode), ReaderView.PROP_PAGE_VIEW_MODE).add(mViewModes, mViewModeTitles).setDefaultValue("1"));
		mOptionsPage.add( new StatusBarOption(getString(R.string.options_page_titlebar)));
		mOptionsPage.add(new BoolOption(getString(R.string.options_page_footnotes), ReaderView.PROP_FOOTNOTES).setDefaultValue("1"));
		//mOptionsPage.add(new ListOption(getString(R.string.options_page_orientation), ReaderView.PROP_ROTATE_ANGLE).add(mOrientations, mOrientationsTitles).setDefaultValue("0"));
		mOptionsPage.add(new ListOption(getString(R.string.options_page_orientation), ReaderView.PROP_APP_SCREEN_ORIENTATION).add(mOrientations, mOrientationsTitles).setDefaultValue("0").setIconId(android.R.drawable.ic_menu_always_landscape_portrait));
		mOptionsPage.add(new ListOption(getString(R.string.options_page_landscape_pages), ReaderView.PROP_LANDSCAPE_PAGES).add(mLandscapePages, mLandscapePagesTitles).setDefaultValue("1"));
		mOptionsPage.add(new ListOption(getString(R.string.options_page_animation), ReaderView.PROP_PAGE_ANIMATION).add(mAnimation, mAnimationTitles).setDefaultValue("1"));
		
		mOptionsPage.add(new ListOption(getString(R.string.options_page_margin_left), ReaderView.PROP_PAGE_MARGIN_LEFT).add(mMargins).setDefaultValue("5"));
		mOptionsPage.add(new ListOption(getString(R.string.options_page_margin_right), ReaderView.PROP_PAGE_MARGIN_RIGHT).add(mMargins).setDefaultValue("5"));
		mOptionsPage.add(new ListOption(getString(R.string.options_page_margin_top), ReaderView.PROP_PAGE_MARGIN_TOP).add(mMargins).setDefaultValue("5"));
		mOptionsPage.add(new ListOption(getString(R.string.options_page_margin_bottom), ReaderView.PROP_PAGE_MARGIN_BOTTOM).add(mMargins).setDefaultValue("5"));
		mOptionsApplication = new OptionsListView(getContext());
		mOptionsApplication.add(new BoolOption(getString(R.string.options_app_fullscreen), ReaderView.PROP_APP_FULLSCREEN));
		mOptionsApplication.add(new BoolOption(getString(R.string.options_app_show_cover_pages), ReaderView.PROP_APP_SHOW_COVERPAGES));
		mOptionsApplication.add(new BoolOption(getString(R.string.options_controls_enable_volume_keys), ReaderView.PROP_CONTROLS_ENABLE_VOLUME_KEYS).setDefaultValue("1"));
		mOptionsApplication.add(new TapZoneOption(getString(R.string.options_app_tapzones_normal), ReaderView.PROP_APP_TAP_ZONE_ACTIONS_TAP));
		mOptionsApplication.add(new KeyMapOption(getString(R.string.options_app_key_actions)));
		mOptionsApplication.add(new BoolOption(getString(R.string.options_app_double_tap_selection), ReaderView.PROP_APP_DOUBLE_TAP_SELECTION).setDefaultValue("0"));
		mOptionsApplication.add(new BoolOption(getString(R.string.options_app_tapzone_hilite), ReaderView.PROP_APP_TAP_ZONE_HILIGHT).setDefaultValue("0"));
		mOptionsApplication.add(new BoolOption(getString(R.string.options_app_trackball_disable), ReaderView.PROP_APP_TRACKBALL_DISABLED).setDefaultValue("0"));
		mOptionsApplication.add(new BoolOption(getString(R.string.options_app_scan_book_props), ReaderView.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED).setDefaultValue("1"));
		mOptionsApplication.add(new BoolOption(getString(R.string.options_app_backlight_lock_enabled), ReaderView.PROP_APP_SCREEN_BACKLIGHT_LOCK).setDefaultValue("1"));
		
		
		mOptionsStyles.refresh();
		mOptionsPage.refresh();
		mOptionsApplication.refresh();
		
		mOptionsControls = new OptionsListView(getContext());
		mOptionsControls.add(new BoolOption("Sample option", "controls.sample"));
		TabHost.TabSpec tsStyles = mTabs.newTabSpec("Styles");
		tsStyles.setIndicator("", //getContext().getResources().getString(R.string.tab_options_styles) 
				getContext().getResources().getDrawable(android.R.drawable.ic_menu_view)); //R.drawable.cr3_option_style
		tsStyles.setContent(this);
		mTabs.addTab(tsStyles);
		TabHost.TabSpec tsPage = mTabs.newTabSpec("Page");
		//getContext().getResources().getString(R.string.tab_options_page)
		tsPage.setIndicator("", getContext().getResources().getDrawable(android.R.drawable.ic_menu_crop)); //R.drawable.cr3_option_page
		tsPage.setContent(this);
		mTabs.addTab(tsPage);
		TabHost.TabSpec tsApp = mTabs.newTabSpec("App");
		//tsApp.setIndicator(null, getContext().getResources().getDrawable(R.drawable.cr3_option_));
		tsApp.setIndicator("", //getContext().getResources().getString(R.string.tab_options_app), 
				getContext().getResources().getDrawable(android.R.drawable.ic_menu_manage));
		tsApp.setContent(this);
		mTabs.addTab(tsApp);
		
		TabHost.TabSpec tsControls = mTabs.newTabSpec("Controls");
		tsControls.setIndicator(getContext().getResources().getString(R.string.tab_options_controls), getContext().getResources().getDrawable(android.R.drawable.ic_menu_manage));
		tsControls.setContent(this);
		//mTabs.addTab(tsControls);

		setView(mTabs);
		
		mOldProperties = new Properties(mProperties);
		
		setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface dialog) {
				askApply();
			}
		});
		
		super.onCreate(savedInstanceState);
		Log.v("cr3", "OptionsDialog is created");
	}

	private void askApply()
	{
		Properties diff = mProperties.diff(mOldProperties);
		if ( diff.size()>0 ) {
			Log.d("cr3", "Some properties were changed, ask user whether to apply");
			AlertDialog.Builder dlg = new AlertDialog.Builder(getContext());
			dlg.setTitle(R.string.win_title_options_apply);
			dlg.setPositiveButton(R.string.dlg_button_ok, new OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					onPositiveButtonClick();
				}
			});
			dlg.setNegativeButton(R.string.dlg_button_cancel, new OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					onNegativeButtonClick();
				}
			});
			dlg.show();
		}
	}
	
	@Override
	protected void onPositiveButtonClick() {
        mReaderView.setSettings(mProperties, mOldProperties);
        dismiss();
        //super.onPositiveButtonClick();
	}
	@Override
	protected void onStop() {
		Log.d("cr3", "OptionsDialog.onStop() : calling gc()");
		System.gc();
		super.onStop();
	}

	
}
