SDLWebViewer for Android
====
# このアプリについて
[SDL（Smart Device Link）](https://smartdevicelink.com/)を使用したデモアプリです。

プロジェクションモードで動作し、WebViewを用いて指定されたWebページを表示させる事ができます。<br>
このアプリは、[SDLBootCamp](https://qiita.com/masaya3/items/cc43ddda428701bc063e)で動作させる事を前提としています。


# HTMLからの車両情報の取得について

車両情報の取得方法は２種類あります。<br>
すべてJavascriptで処理されます。

1. 全ての車両情報の取得
2. 更新された車両情報のみ取得

#### 全ての車両情報の取得方法

以下のメソッドをコールすると、取得可能なすべての車両情報を取得する事ができます。
```javascript
sdl.getVehicleData();
```

#### 更新された車両情報の取得方法
特に設定は不要です。


#### 車両情報の取得方法

車両情報が、正しく取得された場合は、以下のメソッドがコールされます。<br>
車両情報はJSON形式で取得可能です。

```javascript
function getVehicleData(json){
   var vehicleData = JSON.parse(json);
   ...
}
```

#### 車両情報のフォーマットについて
以下の形式でデータが届きます。<br>
細かな内容については、[こちら](https://qiita.com/masaya3/items/ac49a630f1d7cd2eb22f)のページをご確認ください。
```javascript
{
	"gps": {
		"altitude": 41.572509765625,
		"latitudeDegrees": 34.44635682,
		"longitudeDegrees": 135.35284342
	},
	"speed": 88.88,
	"rpm": 9999,
	"fuelLevel": 55.55,
	"fuelRange": [
		{
			"range": 22.22,
			"type": "GASOLINE"
		}
	],
	"instantFuelConsumption": 11.11,
	"externalTemperature": 23.23,
	"vin": "52-452-52-752",
	"tirePressure": {
		"leftRear": {
			"status": "NORMAL",
			"pressure": 240,
			"tpms": "SYSTEM_ACTIVE"
		},
		"innerLeftRear": {
			"status": "NORMAL",
			"pressure": 240,
			"tpms": "SYSTEM_ACTIVE"
		},
		"leftFront": {
			"status": "NORMAL",
			"pressure": 240,
			"tpms": "SYSTEM_ACTIVE"
		},
		"pressureTellTale": "ON",
		"rightFront": {
			"status": "NORMAL",
			"pressure": 240,
			"tpms": "SYSTEM_ACTIVE"
		},
		"innerRightRear": {
			"status": "NORMAL",
			"pressure": 240,
			"tpms": "SYSTEM_ACTIVE"
		}
	},
	"odometer": 7777,
	"beltStatus": {
		"leftRow3BuckleBelted": "YES",
		"passengerBuckleBelted": "YES",
		"driverBeltDeployed": "YES",
		"rightRow3BuckleBelted": "YES",
		"middleRow3BuckleBelted": "YES",
		"driverBuckleBelted": "YES",
		"middleRow1BeltDeployed": "YES",
		"leftRow2BuckleBelted": "YES",
		"rearInflatableBelted": "YES",
		"middleRow1BuckleBelted": "YES",
		"rightRow2BuckleBelted": "YES",
		"passengerChildDetected": "YES",
		"rightRearInflatableBelted": "YES",
		"passengerBeltDeployed": "YES",
		"middleRow2BuckleBelted": "YES"
	},
	"bodyInformation": {
		"rearLeftDoorAjar": true,
		"parkBrakeActive": true,
		"driverDoorAjar": true,
		"rearRightDoorAjar": true,
		"ignitionStableStatus": "IGNITION_SWITCH_STABLE",
		"passengerDoorAjar": true,
		"ignitionStatus": "RUN"
	},
	"deviceStatus": {
		"textMsgAvailable": true,
		"phoneRoaming": true,
		"voiceRecOn": true,
		"signalLevelStatus": "ZERO_LEVEL_BARS",
		"stereoAudioOutputMuted": true,
		"battLevelStatus": "ZERO_LEVEL_BARS",
		"primaryAudioSource": "USB",
		"monoAudioOutputMuted": true,
		"eCallEventActive": true,
		"callActive": true,
		"btIconOn": true
	},
	"driverBraking": "YES",
	"wiperStatus": "AUTO_HIGH",
	"headLampStatus": {
		"lowBeamsOn": true,
		"ambientLightSensorStatus": "NIGHT",
		"highBeamsOn": true
	},
	"engineTorque": 1111.11,
	"engineOilLife": 55.55,
	"accPedalPosition": 44.44,
	"steeringWheelAngle": 128,
	"airbagStatus": {
		"driverAirbagDeployed": "YES",
		"driverKneeAirbagDeployed": "YES",
		"passengerAirbagDeployed": "YES",
		"passengerSideAirbagDeployed": "YES",
		"passengerCurtainAirbagDeployed": "YES",
		"driverSideAirbagDeployed": "YES",
		"driverCurtainAirbagDeployed": "YES",
		"passengerKneeAirbagDeployed": "YES"
	},
	"eCallInfo": {
		"eCallConfirmationStatus": "CALL_IN_PROGRESS",
		"eCallNotificationStatus": "ACTIVE",
		"auxECallNotificationStatus": "ACTIVE"
	},
	"emergencyEvent": {
		"maximumChangeVelocity": 100,
		"emergencyEventType": "FRONTAL",
		"fuelCutoffStatus": "TERMINATE_FUEL",
		"multipleEvents": "YES",
		"rolloverEvent": "YES"
	},
	"clusterModeStatus": {
		"powerModeStatus": "KEY_APPROVED_0",
		"powerModeActive": true,
		"carModeStatus": "NORMAL",
		"powerModeQualificationStatus": "POWER_MODE_OK"
	},
	"myKey": {
		"e911Override": "ON"
	},
	"turnSignal": "BOTH",
	"electronicParkBrakeStatus": "CLOSED"
}
```

# アプリの使い方について

車載器の動作に関して、一部アプリから設定可能です。

#### アプリから設定可能な項目
* メインアプリ、および車載の表示先のURL
* 車載器側のボタン（戻る、ホーム、リロード）
* sdl.getVehicleData呼び出し時にダミーデータを入れるかどうか


# 注意
現在、SDLBootCampには以下の問題があります。

* 車載器側の画面を一定回数タッチするとタッチが反応しなくなる。
* 車載器側の画面の下方が反応しない(認識範囲が350pxまで・・？)

