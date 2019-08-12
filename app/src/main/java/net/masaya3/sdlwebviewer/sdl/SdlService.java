package net.masaya3.sdlwebviewer.sdl;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.SdlManager;
import com.smartdevicelink.managers.SdlManagerListener;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.permission.PermissionElement;
import com.smartdevicelink.managers.permission.PermissionStatus;
import com.smartdevicelink.managers.video.VideoStreamManager;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.rpc.AirbagStatus;
import com.smartdevicelink.proxy.rpc.BeltStatus;
import com.smartdevicelink.proxy.rpc.BodyInformation;
import com.smartdevicelink.proxy.rpc.ClusterModeStatus;
import com.smartdevicelink.proxy.rpc.DeviceStatus;
import com.smartdevicelink.proxy.rpc.DisplayCapabilities;
import com.smartdevicelink.proxy.rpc.ECallInfo;
import com.smartdevicelink.proxy.rpc.EmergencyEvent;
import com.smartdevicelink.proxy.rpc.FuelRange;
import com.smartdevicelink.proxy.rpc.GetVehicleData;
import com.smartdevicelink.proxy.rpc.GetVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.HeadLampStatus;
import com.smartdevicelink.proxy.rpc.MyKey;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.SetDisplayLayout;
import com.smartdevicelink.proxy.rpc.SingleTireStatus;
import com.smartdevicelink.proxy.rpc.SubscribeVehicleData;
import com.smartdevicelink.proxy.rpc.TireStatus;
import com.smartdevicelink.proxy.rpc.UnsubscribeVehicleData;
import com.smartdevicelink.proxy.rpc.VideoStreamingCapability;
import com.smartdevicelink.proxy.rpc.enums.AmbientLightStatus;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.CarModeStatus;
import com.smartdevicelink.proxy.rpc.enums.ComponentVolumeStatus;
import com.smartdevicelink.proxy.rpc.enums.DeviceLevelStatus;
import com.smartdevicelink.proxy.rpc.enums.ECallConfirmationStatus;
import com.smartdevicelink.proxy.rpc.enums.ElectronicParkBrakeStatus;
import com.smartdevicelink.proxy.rpc.enums.EmergencyEventType;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.FuelCutoffStatus;
import com.smartdevicelink.proxy.rpc.enums.FuelType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.IgnitionStableStatus;
import com.smartdevicelink.proxy.rpc.enums.IgnitionStatus;
import com.smartdevicelink.proxy.rpc.enums.PowerModeQualificationStatus;
import com.smartdevicelink.proxy.rpc.enums.PowerModeStatus;
import com.smartdevicelink.proxy.rpc.enums.PredefinedLayout;
import com.smartdevicelink.proxy.rpc.enums.PrimaryAudioSource;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.proxy.rpc.enums.TPMS;
import com.smartdevicelink.proxy.rpc.enums.TurnSignal;
import com.smartdevicelink.proxy.rpc.enums.VehicleDataEventStatus;
import com.smartdevicelink.proxy.rpc.enums.VehicleDataNotificationStatus;
import com.smartdevicelink.proxy.rpc.enums.VehicleDataStatus;
import com.smartdevicelink.proxy.rpc.enums.WarningLightStatus;
import com.smartdevicelink.proxy.rpc.enums.WiperStatus;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.streaming.video.VideoStreamingParameters;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.MultiplexTransportConfig;
import com.smartdevicelink.transport.TCPTransportConfig;

import net.masaya3.sdlwebviewer.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SdlService extends Service {

	public static final String ACTION_GET_VEHICLEDATA = "action_get_vhicledata";
	public static final String ACTION_START_SUBSCRIBE_VEHICLEDATA = "action_start_vhicledata";
	public static final String ACTION_STOP_SUBSCRIBE_VEHICLEDATA = "action_stop_vhicledata";

	private static final String TAG 					= "SDL Service";

	private static final String APP_NAME 				= "SDL Display";
	private static final String APP_ID 					= "8678309";

	private static final String ICON_FILENAME 			= "hello_sdl_icon.png";

	private static final int FOREGROUND_SERVICE_ID = 111;


	// variable to create and call functions of the SyncProxy
	private SdlManager sdlManager = null;

	//プロジェクション画面との通信用
	private LocalBroadcastManager broadcastReceiver;

	//プロジェクション画面との通信用
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			switch(intent.getAction()){
				//現在の情報を取得する
				case ACTION_GET_VEHICLEDATA:
					getVehicleData();
					break;
				//Subscribeを登録する
				case ACTION_START_SUBSCRIBE_VEHICLEDATA:
					startSubscribeVehicleData();
					break;
				//Subscribeを解除する
				case ACTION_STOP_SUBSCRIBE_VEHICLEDATA:
					stopSubscribeVehicleData();
					break;
			}
		}
	};


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		super.onCreate();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			enterForeground();
		}
	}

	// Helper method to let the service enter foreground mode
	@SuppressLint("NewApi")
	public void enterForeground() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(APP_ID, "SdlService", NotificationManager.IMPORTANCE_DEFAULT);
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			if (notificationManager != null) {
				notificationManager.createNotificationChannel(channel);
				Notification serviceNotification = new Notification.Builder(this, channel.getId())
						.setContentTitle("Connected through SDL")
						.setSmallIcon(R.drawable.ic_sdl)
						.build();
				startForeground(FOREGROUND_SERVICE_ID, serviceNotification);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startProxy();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			stopForeground(true);
		}

		if (sdlManager != null) {
			sdlManager.dispose();
		}

		super.onDestroy();
	}

	private void startProxy() {

		if (sdlManager == null) {
			Log.i(TAG, "Starting SDL Proxy");

			String transport_config = "";
			String securiy_config = "";

			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			if(sharedPreferences.getBoolean("use_wifi", false)){
				transport_config  = "TCP";
			}
			else{
				String usbType = "multi_high_bandwidth";
				switch(usbType){
					case "multi_sec_off":
						transport_config = "MULTI";
						securiy_config = "OFF";
						break;
					case "multi_sec_low":
						transport_config = "MULTI";
						securiy_config = "LOW";

						break;
					case "multi_sec_med":
						transport_config = "MULTI";
						securiy_config = "MED";

						break;
					case "multi_sec_high":
						transport_config = "MULTI";
						securiy_config = "HIGH";

						break;
					case "multi_high_bandwidth":
						transport_config = "MULTI_HB";
						break;

				}
			}
			BaseTransportConfig transport = null;
			switch(transport_config){
				case "MULTI":
					int securityLevel;

					switch (securiy_config){
						case "HIGH":
							securityLevel = MultiplexTransportConfig.FLAG_MULTI_SECURITY_HIGH;
							break;
						case "MED":
							securityLevel = MultiplexTransportConfig.FLAG_MULTI_SECURITY_MED;
							break;
						case "LOW":
							securityLevel = MultiplexTransportConfig.FLAG_MULTI_SECURITY_LOW;
							break;
						default:
							securityLevel = MultiplexTransportConfig.FLAG_MULTI_SECURITY_OFF;
							break;
					}
					transport = new MultiplexTransportConfig(this, APP_ID, securityLevel);
					break;
				case "MULTI_HB":
					MultiplexTransportConfig mtc = new MultiplexTransportConfig(this, APP_ID, MultiplexTransportConfig.FLAG_MULTI_SECURITY_OFF);
					mtc.setRequiresHighBandwidth(true);
					transport = mtc;
					break;
				case "TCP":
					String ip = sharedPreferences.getString("sdl_ip_address", getString(R.string.sdlbootcamp_address));

					int port = Integer.parseInt(getString(R.string.sdlbootcamp_port));
					try {
						String port_str = sharedPreferences.getString("sdl_port", getString(R.string.sdlbootcamp_port));
						port = Integer.parseInt(port_str);
					}
					catch (Exception e){
						e.printStackTrace();
					}

					transport = new TCPTransportConfig(port, ip, true);
					break;
			}

			// NAVIGATIONにしておく
			Vector<AppHMIType> appType = new Vector<>();
			appType.add(AppHMIType.NAVIGATION);

			// The manager listener helps you know when certain events that pertain to the SDL Manager happen
			// Here we will listen for ON_HMI_STATUS and ON_COMMAND notifications
			SdlManagerListener listener = new SdlManagerListener() {
				@Override
				public void onStart() {
					// HMI Status Listener
					sdlManager.addOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, new OnRPCNotificationListener() {
						@Override
						public void onNotified(RPCNotification notification) {

							OnHMIStatus status = null;
							if(notification instanceof  OnHMIStatus){
								status = (OnHMIStatus)notification;
							}

							//初回起動時の処理
							if (status.getHmiLevel() == HMILevel.HMI_FULL && ((OnHMIStatus) notification).getFirstRun()) {

								SetDisplayLayout setDisplayLayoutRequest = new SetDisplayLayout();
								//SDLBootCampでは必須の設定
								setDisplayLayoutRequest.setDisplayLayout(PredefinedLayout.NAV_FULLSCREEN_MAP.toString());
								sdlManager.sendRPC(setDisplayLayoutRequest);

								if (sdlManager.getVideoStreamManager() != null) {

									//VideoStreamが有効になったら、プロジェクションを開始する
									sdlManager.getVideoStreamManager().start(new CompletionListener() {
										@Override
										public void onComplete(boolean success) {
											if (success) {
												startProjectionMode();
											} else {
												Log.e(TAG, "Failed to start video streaming manager");
											}
										}
									});
								}
							}

							//HMIが終了したらプロジェクションを終了する
							if (status != null && status.getHmiLevel() == HMILevel.HMI_NONE) {
								stopProjectionMode();
							}
						}
					});

					sdlManager.addOnRPCNotificationListener(FunctionID.ON_VEHICLE_DATA, onRPCNotificationListener);

				}

				@Override
				public void onDestroy() {
					SdlService.this.stopSelf();
				}

				@Override
				public void onError(String info, Exception e) {
				}
			};

			// Create App Icon, this is set in the SdlManager builder
			SdlArtwork appIcon = new SdlArtwork(ICON_FILENAME, FileType.GRAPHIC_PNG, R.mipmap.ic_launcher, true);

			// The manager builder sets options for your session
			SdlManager.Builder builder = new SdlManager.Builder(this, APP_ID, APP_NAME, listener);
			builder.setAppTypes(appType);
			builder.setTransportType(transport);
			builder.setAppIcon(appIcon);
			sdlManager = builder.build();
			sdlManager.start();


		}
	}

	/**
	 * プロジェクションモードを開始する
	 */
	private void startProjectionMode(){

		if(sdlManager == null){
			return;
		}

		//画面のサイズをとる
		Object object = sdlManager.getSystemCapabilityManager().getCapability(SystemCapabilityType.VIDEO_STREAMING);
		if(object instanceof VideoStreamingCapability){
			VideoStreamingCapability capability = (VideoStreamingCapability)object;
			Log.i(TAG, String.format("Display size Width:%d Height:%d",
					capability.getPreferredResolution().getResolutionWidth(),
					capability.getPreferredResolution().getResolutionHeight()));
		}

		VideoStreamingParameters parameters = new VideoStreamingParameters();

		Resources res = getApplicationContext().getResources();

		// 画面サイズの指定
		parameters.getResolution().setResolutionWidth(res.getInteger(R.integer.sdlbootcamp_display_width));
		parameters.getResolution().setResolutionHeight(res.getInteger(R.integer.sdlbootcamp_display_height));

		sdlManager.getVideoStreamManager().startRemoteDisplayStream(getApplicationContext(), ProjectionDisplay.class, parameters, false);
	}

	/**
	 * プロジェクションモードを停止する
	 */
	private void stopProjectionMode() {

		if(sdlManager == null){
			return;
		}

		VideoStreamManager manager = sdlManager.getVideoStreamManager();
		if(manager != null){
			manager.stopStreaming();
		}
	}

	/**
	 * 利用可能なテンプレートをチェックする
	 */
	private void checkTemplateType(){

		Object result = sdlManager.getSystemCapabilityManager().getCapability(SystemCapabilityType.DISPLAY);
		if( result instanceof DisplayCapabilities){
			List<String> templates = ((DisplayCapabilities) result).getTemplatesAvailable();

			Log.i("Templete", templates.toString());

		}
	}

	/**
	 * 利用する項目が利用可能かどうか
	 */
	private void checkPermission(){
		List<PermissionElement> permissionElements = new ArrayList<>();

		//チェックを行う項目
		List<String> keys = new ArrayList<>();
		keys.add(GetVehicleData.KEY_SPEED);
		keys.add(GetVehicleData.KEY_RPM);
		keys.add(GetVehicleData.KEY_FUEL_LEVEL);
		keys.add(GetVehicleData.KEY_FUEL_RANGE);
		keys.add(GetVehicleData.KEY_INSTANT_FUEL_CONSUMPTION);
		keys.add(GetVehicleData.KEY_EXTERNAL_TEMPERATURE);
		keys.add(GetVehicleData.KEY_VIN);
		keys.add(GetVehicleData.KEY_TIRE_PRESSURE);
		keys.add(GetVehicleData.KEY_ODOMETER);
		keys.add(GetVehicleData.KEY_BELT_STATUS);
		keys.add(GetVehicleData.KEY_BODY_INFORMATION);
		keys.add(GetVehicleData.KEY_DEVICE_STATUS);
		keys.add(GetVehicleData.KEY_DRIVER_BRAKING);
		keys.add(GetVehicleData.KEY_WIPER_STATUS);
		keys.add(GetVehicleData.KEY_HEAD_LAMP_STATUS);
		keys.add(GetVehicleData.KEY_ENGINE_TORQUE);
		keys.add(GetVehicleData.KEY_ENGINE_OIL_LIFE);
		keys.add(GetVehicleData.KEY_ACC_PEDAL_POSITION);
		keys.add(GetVehicleData.KEY_STEERING_WHEEL_ANGLE);
		keys.add(GetVehicleData.KEY_AIRBAG_STATUS);
		keys.add(GetVehicleData.KEY_E_CALL_INFO);
		keys.add(GetVehicleData.KEY_EMERGENCY_EVENT);
		keys.add(GetVehicleData.KEY_CLUSTER_MODE_STATUS);
		keys.add(GetVehicleData.KEY_MY_KEY);
		keys.add(GetVehicleData.KEY_TURN_SIGNAL);
		keys.add(GetVehicleData.KEY_ELECTRONIC_PARK_BRAKE_STATUS);

		permissionElements.add(new PermissionElement(FunctionID.GET_VEHICLE_DATA, keys));

		Map<FunctionID, PermissionStatus> status = sdlManager.getPermissionManager().getStatusOfPermissions(permissionElements);

		//すべてが許可されているかどうか
		Log.i("Permission", "Allowed:" + status.get(FunctionID.GET_VEHICLE_DATA).getIsRPCAllowed());

		Map<String, Boolean> permissionMap = status.get(FunctionID.GET_VEHICLE_DATA).getAllowedParameters();
		//各項目ごとの状況を表示
		for(String key : keys){
			Log.i("Permission",  String.format("%s Allowed:", permissionMap.get(key)));
		}
	}

	/**
	 * 指定された車情報を取得する
	 */
	private void getVehicleData(){

		if(sdlManager == null){
			return;
		}

		GetVehicleData vdRequest = new GetVehicleData();

		//取得する車情報を設定する(すべての情報）
		vdRequest.setGps(true);							//緯度、経度、速度などのGPSデータ
		vdRequest.setSpeed(true);						//車速(KPH)
		vdRequest.setRpm(true);							//エンジンの毎分回転数
		vdRequest.setFuelLevel(true);					//タンク内の燃料レベル（パーセント）
		vdRequest.setFuelRange(true);					//燃費
		vdRequest.setInstantFuelConsumption(true);		//瞬間的な燃料消費量
		vdRequest.setExternalTemperature(true);			//摂氏温度での外部温度
		vdRequest.setVin(true);							//車両識別番号
		vdRequest.setTirePressure(true);				//選択されたギア
		vdRequest.setOdometer(true);					//走行距離計(Km
		vdRequest.setBeltStatus(true);					//各シートベルトの状態
		vdRequest.setBodyInformation(true);				//各車体の状態
		vdRequest.setDeviceStatus(true);				//スマートフォンデバイスに関する情報
		vdRequest.setDriverBraking(true);				//ブレーキペダルの状態
		vdRequest.setWiperStatus(true);					//ワイパーの状態
		vdRequest.setHeadLampStatus(true);				//ヘッドランプの状態
		vdRequest.setEngineTorque(true);				//ディーゼル以外のエンジンのトルク値（Nm）
		vdRequest.setEngineOilLife(true);				//エンジンの残存オイル寿命の推定割合
		vdRequest.setAccPedalPosition(true);			//アクセルペダルの位置
		vdRequest.setSteeringWheelAngle(true);			//ステアリングホイールの現在の角度
		vdRequest.setAirbagStatus(true);				//車両内の各エアバッグの状態
		vdRequest.setECallInfo(true);					//緊急通報の状況に関する情報
		vdRequest.setEmergencyEvent(true);				//緊急の状況
		vdRequest.setClusterModeStatus(true);			//電力モードがアクティブかどうか
		vdRequest.setMyKey(true);						//緊急911などの情報
		vdRequest.setTurnSignal(true);					//方向指示器の状態
		vdRequest.setElectronicParkBrakeStatus(true);	//パークブレーキのステータス


		vdRequest.setOnRPCResponseListener(new OnRPCResponseListener() {

			@Override
			public void onResponse(int correlationId, RPCResponse response) {

				JSONObject json = new JSONObject();

				if(!response.getSuccess()){
					Log.i("SdlService", "GetVehicleData was rejected.");
				}

				boolean isDummy = false;

				GetVehicleDataResponse vehicleData =  (GetVehicleDataResponse)response;
				try {

					//GPS(ここは自分のGPS情報を出力する

					//Speed
					Double speed = vehicleData.getSpeed();
					if(speed == null && isDummy){
						speed = 88.88;
					}
					if (speed!= null) {
						json.put(GetVehicleDataResponse.KEY_SPEED, speed);
					}

					//RPM
					Integer rpm =  vehicleData.getRpm();
					if(rpm == null && isDummy){
						rpm = 9999;
					}
					if(rpm != null){
						json.put(GetVehicleDataResponse.KEY_RPM, rpm);
					}

					Double fuelLevel = vehicleData.getFuelLevel();
					if(fuelLevel == null&& isDummy){
						fuelLevel = 55.55;
					}
					if(fuelLevel != null){
						json.put(GetVehicleDataResponse.KEY_FUEL_LEVEL, fuelLevel);
					}

					List<FuelRange> ranges = vehicleData.getFuelRange();
					if(ranges == null && isDummy){
						ranges = new ArrayList<FuelRange>();
						FuelRange range = new FuelRange();
						range.setType(FuelType.GASOLINE);
						range.setRange(22.22f);
						ranges.add(range);
					}

					if(ranges != null){
						JSONArray range_array = new JSONArray();
						for(FuelRange range : ranges){
							range_array.put(range.serializeJSON());
						}
						json.put(GetVehicleDataResponse.KEY_FUEL_RANGE, range_array);
					}

					Double instantFuel = vehicleData.getInstantFuelConsumption();
					if(instantFuel == null && isDummy){
						instantFuel = 11.11;
					}
					if(instantFuel != null){
						json.put(GetVehicleDataResponse.KEY_INSTANT_FUEL_CONSUMPTION, instantFuel);
					}

					Double temprature =  vehicleData.getExternalTemperature();
					if(temprature == null && isDummy){
						temprature = 23.23;
					}
					if(temprature != null){
						json.put(GetVehicleDataResponse.KEY_EXTERNAL_TEMPERATURE, temprature);
					}

					String vin = vehicleData.getVin();
					if(vin == null && isDummy){
						vin = "DUMMY_VIN";
					}
					if(vin != null){
						json.put(GetVehicleDataResponse.KEY_VIN, vin);
					}

					TireStatus tireStatus = vehicleData.getTirePressure();
					if(tireStatus == null && isDummy){
						tireStatus = new TireStatus();

						SingleTireStatus status = new SingleTireStatus();
						status.setPressure(240.0f);
						status.setStatus(ComponentVolumeStatus.NORMAL);
						status.setTPMS(TPMS.SYSTEM_ACTIVE);

						tireStatus.setInnerLeftRear(status);
						tireStatus.setInnerRightRear(status);
						tireStatus.setLeftFront(status);
						tireStatus.setRightFront(status);
						tireStatus.setLeftRear(status);
						tireStatus.setRightFront(status);

						tireStatus.setPressureTellTale(WarningLightStatus.ON);

					}
					if(tireStatus != null){
						json.put(GetVehicleDataResponse.KEY_TIRE_PRESSURE, tireStatus.serializeJSON());
					}

					Integer odometer = vehicleData.getOdometer();
					if(odometer == null && isDummy){
						odometer = 7777;
					}
					if(odometer != null){
						json.put(GetVehicleDataResponse.KEY_ODOMETER, odometer);
					}

					BeltStatus beltStatus = vehicleData.getBeltStatus();
					if(beltStatus == null && isDummy){
						beltStatus = new BeltStatus();
						beltStatus.setDriverBeltDeployed(VehicleDataEventStatus.YES);
						beltStatus.setDriverBuckleBelted(VehicleDataEventStatus.YES);
						beltStatus.setLeftRearInflatableBelted(VehicleDataEventStatus.YES);
						beltStatus.setLeftRow2BuckleBelted(VehicleDataEventStatus.YES);
						beltStatus.setLeftRow3BuckleBelted(VehicleDataEventStatus.YES);
						beltStatus.setRightRearInflatableBelted(VehicleDataEventStatus.YES);
						beltStatus.setRightRow2BuckleBelted(VehicleDataEventStatus.YES);
						beltStatus.setRightRow3BuckleBelted(VehicleDataEventStatus.YES);
						beltStatus.setMiddleRow1BeltDeployed(VehicleDataEventStatus.YES);
						beltStatus.setMiddleRow1BuckleBelted(VehicleDataEventStatus.YES);
						beltStatus.setMiddleRow2BuckleBelted(VehicleDataEventStatus.YES);
						beltStatus.setMiddleRow3BuckleBelted(VehicleDataEventStatus.YES);
						beltStatus.setPassengerBeltDeployed(VehicleDataEventStatus.YES);
						beltStatus.setPassengerBuckleBelted(VehicleDataEventStatus.YES);
						beltStatus.setPassengerChildDetected(VehicleDataEventStatus.YES);

					}
					if(beltStatus != null){
						json.put(GetVehicleDataResponse.KEY_BELT_STATUS, beltStatus.serializeJSON());
					}

					BodyInformation bodyInformation = vehicleData.getBodyInformation();
					if(bodyInformation == null && isDummy){
						bodyInformation = new BodyInformation();
						bodyInformation.setDriverDoorAjar(true);
						bodyInformation.setIgnitionStableStatus(IgnitionStableStatus.IGNITION_SWITCH_STABLE);
						bodyInformation.setIgnitionStatus(IgnitionStatus.RUN);
						bodyInformation.setParkBrakeActive(true);
						bodyInformation.setPassengerDoorAjar(true);
						bodyInformation.setRearLeftDoorAjar(true);
						bodyInformation.setRearRightDoorAjar(true);
					}
					if(bodyInformation != null){
						json.put(GetVehicleDataResponse.KEY_BODY_INFORMATION, bodyInformation.serializeJSON());
					}

					DeviceStatus deviceStatus = vehicleData.getDeviceStatus();
					if(deviceStatus == null && isDummy){
						deviceStatus = new DeviceStatus();
						deviceStatus.setBattLevelStatus(DeviceLevelStatus.ZERO_LEVEL_BARS);
						deviceStatus.setBtIconOn(true);
						deviceStatus.setCallActive(true);
						deviceStatus.setECallEventActive(true);
						deviceStatus.setMonoAudioOutputMuted(true);
						deviceStatus.setPhoneRoaming(true);
						deviceStatus.setPrimaryAudioSource(PrimaryAudioSource.USB);
						deviceStatus.setSignalLevelStatus(DeviceLevelStatus.ZERO_LEVEL_BARS);
						deviceStatus.setStereoAudioOutputMuted(true);
						deviceStatus.setTextMsgAvailable(true);
						deviceStatus.setVoiceRecOn(true);
					}
					if(deviceStatus != null){
						json.put(GetVehicleDataResponse.KEY_DEVICE_STATUS, deviceStatus.serializeJSON());
					}

					VehicleDataEventStatus driverBraking = vehicleData.getDriverBraking();
					if(driverBraking == null && isDummy){
						driverBraking = VehicleDataEventStatus.YES;
					}
					if(driverBraking != null){
						json.put(GetVehicleDataResponse.KEY_DRIVER_BRAKING, driverBraking.toString());
					}

					WiperStatus wiperStatus = vehicleData.getWiperStatus();
					if(wiperStatus == null && isDummy){
						wiperStatus = WiperStatus.AUTO_HIGH;
					}

					if(wiperStatus != null){
						json.put(GetVehicleDataResponse.KEY_WIPER_STATUS, wiperStatus.toString());
					}

					HeadLampStatus headLampStatus = vehicleData.getHeadLampStatus();
					if(headLampStatus == null && isDummy){
						headLampStatus = new HeadLampStatus();
						headLampStatus.setAmbientLightStatus(AmbientLightStatus.NIGHT);
						headLampStatus.setHighBeamsOn(true);
						headLampStatus.setLowBeamsOn(true);
					}

					if(headLampStatus != null){
						json.put(GetVehicleDataResponse.KEY_HEAD_LAMP_STATUS, headLampStatus.serializeJSON());
					}

					Double engineTorque = vehicleData.getEngineTorque();
					if(engineTorque == null && isDummy){
						engineTorque = 1111.11;
					}

					if(engineTorque != null){
						json.put(GetVehicleDataResponse.KEY_ENGINE_TORQUE, engineTorque);
					}

					Float engineOilLife = vehicleData.getEngineOilLife();
					if(engineOilLife == null && isDummy){
						engineOilLife = 55.55f;
					}

					if(engineOilLife != null){
						json.put(GetVehicleDataResponse.KEY_ENGINE_OIL_LIFE, engineOilLife);
					}

					Double accPedalPosition = vehicleData.getAccPedalPosition();
					if(accPedalPosition == null && isDummy){
						accPedalPosition = 44.44;
					}

					if(accPedalPosition != null){
						json.put(GetVehicleDataResponse.KEY_ACC_PEDAL_POSITION, accPedalPosition);
					}

					Double steeringWheelAngle = vehicleData.getSteeringWheelAngle();
					if(steeringWheelAngle == null && isDummy){
						steeringWheelAngle = 128.0;
					}

					if(steeringWheelAngle != null){
						json.put(GetVehicleDataResponse.KEY_STEERING_WHEEL_ANGLE, steeringWheelAngle);
					}

					AirbagStatus airbagStatus = vehicleData.getAirbagStatus();
					if(airbagStatus == null && isDummy){
						airbagStatus = new AirbagStatus();
						airbagStatus.setDriverAirbagDeployed(VehicleDataEventStatus.YES);
						airbagStatus.setDriverCurtainAirbagDeployed(VehicleDataEventStatus.YES);
						airbagStatus.setDriverKneeAirbagDeployed(VehicleDataEventStatus.YES);
						airbagStatus.setDriverSideAirbagDeployed(VehicleDataEventStatus.YES);
						airbagStatus.setPassengerAirbagDeployed(VehicleDataEventStatus.YES);
						airbagStatus.setPassengerCurtainAirbagDeployed(VehicleDataEventStatus.YES);
						airbagStatus.setPassengerKneeAirbagDeployed(VehicleDataEventStatus.YES);
						airbagStatus.setPassengerSideAirbagDeployed(VehicleDataEventStatus.YES);
					}

					if(airbagStatus != null){
						json.put(GetVehicleDataResponse.KEY_AIRBAG_STATUS, airbagStatus.serializeJSON());
					}

					ECallInfo eCallInfo = vehicleData.getECallInfo();
					if(eCallInfo == null && isDummy){
						eCallInfo = new ECallInfo();
						eCallInfo.setAuxECallNotificationStatus(VehicleDataNotificationStatus.ACTIVE);
						eCallInfo.setECallConfirmationStatus(ECallConfirmationStatus.CALL_IN_PROGRESS);
						eCallInfo.setECallNotificationStatus(VehicleDataNotificationStatus.ACTIVE);
					}

					if(eCallInfo != null){
						json.put(GetVehicleDataResponse.KEY_E_CALL_INFO, eCallInfo.serializeJSON());
					}

					EmergencyEvent emergencyEvent = vehicleData.getEmergencyEvent();
					if(emergencyEvent == null && isDummy){
						emergencyEvent = new EmergencyEvent();
						emergencyEvent.setEmergencyEventType(EmergencyEventType.FRONTAL);
						emergencyEvent.setFuelCutoffStatus(FuelCutoffStatus.TERMINATE_FUEL);
						emergencyEvent.setMaximumChangeVelocity(100);
						emergencyEvent.setMultipleEvents(VehicleDataEventStatus.YES);
						emergencyEvent.setRolloverEvent(VehicleDataEventStatus.YES);
					}

					if(emergencyEvent != null){
						json.put(GetVehicleDataResponse.KEY_EMERGENCY_EVENT, emergencyEvent.serializeJSON());
					}

					ClusterModeStatus clusterModeStatus = vehicleData.getClusterModeStatus();
					if(clusterModeStatus == null && isDummy){
						clusterModeStatus = new ClusterModeStatus();
						clusterModeStatus.setCarModeStatus(CarModeStatus.NORMAL);
						clusterModeStatus.setPowerModeActive(true);
						clusterModeStatus.setPowerModeQualificationStatus(PowerModeQualificationStatus.POWER_MODE_OK);
						clusterModeStatus.setPowerModeStatus(PowerModeStatus.KEY_APPROVED_0);
					}

					if(clusterModeStatus != null){
						json.put(GetVehicleDataResponse.KEY_CLUSTER_MODE_STATUS, clusterModeStatus.serializeJSON());
					}

					MyKey myKey = vehicleData.getMyKey();
					if(myKey == null && isDummy){
						myKey = new MyKey();
						myKey.setE911Override(VehicleDataStatus.ON);
					}

					if(myKey != null){
						json.put(GetVehicleDataResponse.KEY_MY_KEY, myKey.serializeJSON());
					}

					TurnSignal turnSignal = vehicleData.getTurnSignal();
					if(turnSignal == null && isDummy){
						turnSignal = TurnSignal.BOTH;
					}

					if(turnSignal != null){
						json.put(GetVehicleDataResponse.KEY_TURN_SIGNAL, turnSignal.toString());
					}

					ElectronicParkBrakeStatus electronicParkBrakeStatus = vehicleData.getElectronicParkBrakeStatus();
					if(electronicParkBrakeStatus == null && isDummy){
						electronicParkBrakeStatus = ElectronicParkBrakeStatus.CLOSED;
					}

					if(electronicParkBrakeStatus != null){
						json.put(GetVehicleDataResponse.KEY_ELECTRONIC_PARK_BRAKE_STATUS, electronicParkBrakeStatus.toString());
					}


					//プロジェクション画面にデータを送信する
					final Intent intent = new Intent();
					intent.setAction(ProjectionDisplay.ACTION_VEHICLEDATA);
					intent.putExtra("vehicle", json.toString());
					broadcastReceiver.sendBroadcast(intent);

				}
				catch (JSONException e){
					e.printStackTrace();
				}


			}
		});
		sdlManager.sendRPC(vdRequest);
	}


	/**
	 * 指定された車情報の定期取得の開始
	 */
	private void startSubscribeVehicleData(){

		if(sdlManager == null){
			return;
		}

		//定期受信用のデータを設定する
		SubscribeVehicleData subscribeRequest = new SubscribeVehicleData();

		//取得する車情報を設定する(すべての情報）
		subscribeRequest.setGps(true);					//緯度、経度、速度などのGPSデータ
		subscribeRequest.setSpeed(true);					//車速(KPH)
		subscribeRequest.setRpm(true);					//エンジンの毎分回転数
		subscribeRequest.setFuelLevel(true);				//タンク内の燃料レベル（パーセント）
		subscribeRequest.setFuelRange(true);				//燃費
		subscribeRequest.setInstantFuelConsumption(true);	//瞬間的な燃料消費量
		subscribeRequest.setExternalTemperature(true);	//摂氏温度での外部温度
		subscribeRequest.setTirePressure(true);			//選択されたギア
		subscribeRequest.setOdometer(true);				//走行距離計(Km
		subscribeRequest.setBeltStatus(true);				//各シートベルトの状態
		subscribeRequest.setBodyInformation(true);		//各車体の状態
		subscribeRequest.setDeviceStatus(true);			//スマートフォンデバイスに関する情報
		subscribeRequest.setDriverBraking(true);			//ブレーキペダルの状態
		subscribeRequest.setWiperStatus(true);			//ワイパーの状態
		subscribeRequest.setHeadLampStatus(true);			//ヘッドランプの状態
		subscribeRequest.setEngineTorque(true);			//ディーゼル以外のエンジンのトルク値（Nm）
		subscribeRequest.setEngineOilLife(true);			//エンジンの残存オイル寿命の推定割合
		subscribeRequest.setAccPedalPosition(true);		//アクセルペダルの位置
		subscribeRequest.setSteeringWheelAngle(true);		//ステアリングホイールの現在の角度
		subscribeRequest.setAirbagStatus(true);			//車両内の各エアバッグの状態
		subscribeRequest.setECallInfo(true);				//緊急通報の状況に関する情報
		subscribeRequest.setEmergencyEvent(true);			//緊急の状況
		subscribeRequest.setClusterModeStatus(true);		//電力モードがアクティブかどうか
		subscribeRequest.setMyKey(true);					//緊急911などの情報
		subscribeRequest.setTurnSignal(true);				//方向指示器の状態
		subscribeRequest.setElectronicParkBrakeStatus(true);//パークブレーキのステータス

		subscribeRequest.setOnRPCResponseListener(new OnRPCResponseListener() {
			@Override
			public void onResponse(int correlationId, RPCResponse response) {
				if (response.getSuccess()) {
					Log.i("SdlService", "Successfully subscribed to vehicle data.");
				} else {
					Log.i("SdlService", "Request to subscribe to vehicle data was rejected.");
				}
			}
		});

		sdlManager.sendRPC(subscribeRequest);
	}


	/**
	 * 指定された車情報の定期取得の終了
	 */
	private void stopSubscribeVehicleData(){

		if(sdlManager == null){
			return;
		}

		UnsubscribeVehicleData unsubscribeRequest = new UnsubscribeVehicleData();

		//解除する車情報を設定する(すべての情報）
		unsubscribeRequest.setGps(true);					//緯度、経度、速度などのGPSデータ
		unsubscribeRequest.setSpeed(true);					//車速(KPH)
		unsubscribeRequest.setRpm(true);					//エンジンの毎分回転数
		unsubscribeRequest.setFuelLevel(true);				//タンク内の燃料レベル（パーセント）
		unsubscribeRequest.setFuelRange(true);				//燃費
		unsubscribeRequest.setInstantFuelConsumption(true);	//瞬間的な燃料消費量
		unsubscribeRequest.setExternalTemperature(true);	//摂氏温度での外部温度
		unsubscribeRequest.setTirePressure(true);			//選択されたギア
		unsubscribeRequest.setOdometer(true);				//走行距離計(Km
		unsubscribeRequest.setBeltStatus(true);				//各シートベルトの状態
		unsubscribeRequest.setBodyInformation(true);		//各車体の状態
		unsubscribeRequest.setDeviceStatus(true);			//スマートフォンデバイスに関する情報
		unsubscribeRequest.setDriverBraking(true);			//ブレーキペダルの状態
		unsubscribeRequest.setWiperStatus(true);			//ワイパーの状態
		unsubscribeRequest.setHeadLampStatus(true);			//ヘッドランプの状態
		unsubscribeRequest.setEngineTorque(true);			//ディーゼル以外のエンジンのトルク値（Nm）
		unsubscribeRequest.setEngineOilLife(true);			//エンジンの残存オイル寿命の推定割合
		unsubscribeRequest.setAccPedalPosition(true);		//アクセルペダルの位置
		unsubscribeRequest.setSteeringWheelAngle(true);		//ステアリングホイールの現在の角度
		unsubscribeRequest.setAirbagStatus(true);			//車両内の各エアバッグの状態
		unsubscribeRequest.setECallInfo(true);				//緊急通報の状況に関する情報
		unsubscribeRequest.setEmergencyEvent(true);			//緊急の状況
		unsubscribeRequest.setClusterModeStatus(true);		//電力モードがアクティブかどうか
		unsubscribeRequest.setMyKey(true);					//緊急911などの情報
		unsubscribeRequest.setTurnSignal(true);				//方向指示器の状態
		unsubscribeRequest.setElectronicParkBrakeStatus(true);//パークブレーキのステータス

		unsubscribeRequest.setOnRPCResponseListener(new OnRPCResponseListener() {
			@Override
			public void onResponse(int correlationId, RPCResponse response) {
				if(response.getSuccess()){
					Log.i("SdlService", "Successfully unsubscribed to vehicle data.");
				}else{
					Log.i("SdlService", "Request to unsubscribe to vehicle data was rejected.");
				}
			}
		});
		sdlManager.sendRPC(unsubscribeRequest);
	}

	/**
	 * 車情報の定期取得
	 */
	private OnRPCNotificationListener onRPCNotificationListener = new OnRPCNotificationListener() {

		@Override
		public void onNotified(RPCNotification notification) {
			OnVehicleData vehicleData = (OnVehicleData) notification;
			JSONObject json = new JSONObject();

			try {

				//GPS(ここは自分のGPS情報を出力する

				//Speed
				Double speed = vehicleData.getSpeed();
				if (speed!= null) {
					json.put(GetVehicleDataResponse.KEY_SPEED, speed);
				}

				//RPM
				Integer rpm =  vehicleData.getRpm();
				if(rpm != null){
					json.put(GetVehicleDataResponse.KEY_RPM, rpm);
				}

				Double fuelLevel = vehicleData.getFuelLevel();
				if(fuelLevel != null){
					json.put(GetVehicleDataResponse.KEY_FUEL_LEVEL, fuelLevel);
				}

				List<FuelRange> ranges = vehicleData.getFuelRange();
				if(ranges != null){
					JSONArray range_array = new JSONArray();
					for(FuelRange range : ranges){
						range_array.put(range.serializeJSON());
					}
					json.put(GetVehicleDataResponse.KEY_FUEL_RANGE, range_array);
				}

				Double instantFuel = vehicleData.getInstantFuelConsumption();
				if(instantFuel != null){
					json.put(GetVehicleDataResponse.KEY_INSTANT_FUEL_CONSUMPTION, instantFuel);
				}

				Double temprature =  vehicleData.getExternalTemperature();
				if(temprature != null){
					json.put(GetVehicleDataResponse.KEY_EXTERNAL_TEMPERATURE, temprature);
				}

				String vin = vehicleData.getVin();
				if(vin != null){
					json.put(GetVehicleDataResponse.KEY_VIN, vin);
				}

				TireStatus tireStatus = vehicleData.getTirePressure();
				if(tireStatus != null){
					json.put(GetVehicleDataResponse.KEY_TIRE_PRESSURE, tireStatus.serializeJSON());
				}

				Integer odometer = vehicleData.getOdometer();
				if(odometer != null){
					json.put(GetVehicleDataResponse.KEY_ODOMETER, odometer);
				}

				BeltStatus beltStatus = vehicleData.getBeltStatus();
				if(beltStatus != null){
					json.put(GetVehicleDataResponse.KEY_BELT_STATUS, beltStatus.serializeJSON());
				}

				BodyInformation bodyInformation = vehicleData.getBodyInformation();
				if(bodyInformation != null){
					json.put(GetVehicleDataResponse.KEY_BODY_INFORMATION, bodyInformation.serializeJSON());
				}

				DeviceStatus deviceStatus = vehicleData.getDeviceStatus();
				if(deviceStatus != null){
					json.put(GetVehicleDataResponse.KEY_DEVICE_STATUS, deviceStatus.serializeJSON());
				}

				VehicleDataEventStatus driverBraking = vehicleData.getDriverBraking();
				if(driverBraking != null){
					json.put(GetVehicleDataResponse.KEY_DRIVER_BRAKING, driverBraking.toString());
				}

				WiperStatus wiperStatus = vehicleData.getWiperStatus();
				if(wiperStatus != null){
					json.put(GetVehicleDataResponse.KEY_WIPER_STATUS, wiperStatus.toString());
				}

				HeadLampStatus headLampStatus = vehicleData.getHeadLampStatus();
				if(headLampStatus != null){
					json.put(GetVehicleDataResponse.KEY_HEAD_LAMP_STATUS, headLampStatus.serializeJSON());
				}

				Double engineTorque = vehicleData.getEngineTorque();
				if(engineTorque != null){
					json.put(GetVehicleDataResponse.KEY_ENGINE_TORQUE, engineTorque);
				}

				Float engineOilLife = vehicleData.getEngineOilLife();
				if(engineOilLife != null){
					json.put(GetVehicleDataResponse.KEY_ENGINE_OIL_LIFE, engineOilLife);
				}

				Double accPedalPosition = vehicleData.getAccPedalPosition();
				if(accPedalPosition != null){
					json.put(GetVehicleDataResponse.KEY_ACC_PEDAL_POSITION, accPedalPosition);
				}

				Double steeringWheelAngle = vehicleData.getSteeringWheelAngle();
				if(steeringWheelAngle != null){
					json.put(GetVehicleDataResponse.KEY_STEERING_WHEEL_ANGLE, steeringWheelAngle);
				}

				AirbagStatus airbagStatus = vehicleData.getAirbagStatus();
				if(airbagStatus != null){
					json.put(GetVehicleDataResponse.KEY_AIRBAG_STATUS, airbagStatus.serializeJSON());
				}

				ECallInfo eCallInfo = vehicleData.getECallInfo();
				if(eCallInfo != null){
					json.put(GetVehicleDataResponse.KEY_E_CALL_INFO, eCallInfo.serializeJSON());
				}

				EmergencyEvent emergencyEvent = vehicleData.getEmergencyEvent();
				if(emergencyEvent != null){
					json.put(GetVehicleDataResponse.KEY_EMERGENCY_EVENT, emergencyEvent.serializeJSON());
				}

				ClusterModeStatus clusterModeStatus = vehicleData.getClusterModeStatus();
				if(clusterModeStatus != null){
					json.put(GetVehicleDataResponse.KEY_CLUSTER_MODE_STATUS, clusterModeStatus.serializeJSON());
				}

				MyKey myKey = vehicleData.getMyKey();
				if(myKey != null){
					json.put(GetVehicleDataResponse.KEY_MY_KEY, myKey.serializeJSON());
				}

				TurnSignal turnSignal = vehicleData.getTurnSignal();
				if(turnSignal != null){
					json.put(GetVehicleDataResponse.KEY_TURN_SIGNAL, turnSignal.toString());
				}

				ElectronicParkBrakeStatus electronicParkBrakeStatus = vehicleData.getElectronicParkBrakeStatus();
				if(electronicParkBrakeStatus != null){
					json.put(GetVehicleDataResponse.KEY_ELECTRONIC_PARK_BRAKE_STATUS, electronicParkBrakeStatus.toString());
				}

				//プロジェクション画面にデータを送信する
				final Intent intent = new Intent();
				intent.setAction(ProjectionDisplay.ACTION_VEHICLEDATA);
				intent.putExtra("vehicle", json.toString());
				broadcastReceiver.sendBroadcast(intent);

			}
			catch (JSONException e){
				e.printStackTrace();
			}

		}
	};

}
