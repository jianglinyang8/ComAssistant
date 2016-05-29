package com.bjw.ComAssistant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import com.bjw.bean.AssistBean;
import com.bjw.bean.ComBean;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.KeyListener;
import android.text.method.NumberKeyListener;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android_serialport_api.SerialPortFinder;

/**
 * serialport api��jniȡ��http://code.google.com/p/android-serialport-api/
 * @author benjaminwan
 * �������֣�֧��4����ͬʱ��д
 * ��������ʱ�Զ����������豸
 * n,8,1��û��ѡ
 */
public class ComAssistantActivity extends Activity {
	EditText editTextRecDisp,editTextLines,editTextCOMA;
	EditText editTextTimeCOMA;
	CheckBox checkBoxAutoClear,checkBoxAutoCOMA;
	Button ButtonClear,ButtonSendCOMA,leftButton,rightButton,spinButton,forwardButton,backwardButton;
	ToggleButton toggleButtonCOMA;
	Spinner SpinnerCOMA;
	Spinner SpinnerBaudRateCOMA;
	RadioButton radioButtonTxt,radioButtonHex;
	SerialControl ComA;//4������
	DispQueueThread DispQueue;//ˢ����ʾ�߳�
	SerialPortFinder mSerialPortFinder;//�����豸����
	AssistBean AssistData;//���ڽ����������л��ͷ����л�
	int iRecLines=0;//����������
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();
        DispQueue.start();
        AssistData = getAssistData();
        setControls();
    }
    @Override
    public void onDestroy(){
    	saveAssistData(AssistData);
    	CloseComPort(ComA);
    	super.onDestroy();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      CloseComPort(ComA);
      setContentView(R.layout.main);
      setControls();
    }
    
    //----------------------------------------------------
    private void setControls()
	{
    	String appName = getString(R.string.app_name);
        try {
			PackageInfo pinfo = getPackageManager().getPackageInfo("com.bjw.ComAssistant", PackageManager.GET_CONFIGURATIONS);
			String versionName = pinfo.versionName;
//			String versionCode = String.valueOf(pinfo.versionCode);
			setTitle(appName+" V"+versionName);
        } catch (NameNotFoundException e) {
        	e.printStackTrace();
        }
    	editTextRecDisp=(EditText)findViewById(R.id.editTextRecDisp);
    	editTextLines=(EditText)findViewById(R.id.editTextLines);
    	editTextCOMA=(EditText)findViewById(R.id.editTextCOMA);
    	editTextTimeCOMA = (EditText)findViewById(R.id.editTextTimeCOMA);
    	checkBoxAutoClear=(CheckBox)findViewById(R.id.checkBoxAutoClear);
		checkBoxAutoCOMA=(CheckBox)findViewById(R.id.checkBoxAutoCOMA);
    	ButtonClear=(Button)findViewById(R.id.ButtonClear);
    	ButtonSendCOMA=(Button)findViewById(R.id.ButtonSendCOMA);
    	///////////////////////////////////////////////////////////////////////////////////
    	leftButton=(Button)findViewById(R.id.leftButton);
    	rightButton=(Button)findViewById(R.id.rightButton);
    	spinButton=(Button)findViewById(R.id.spinButton);
    	forwardButton=(Button)findViewById(R.id.forwardButton);
    	backwardButton=(Button)findViewById(R.id.backwardButton);
    	///////////////////////////////////////////////////////////////////////////////////
    	toggleButtonCOMA=(ToggleButton)findViewById(R.id.toggleButtonCOMA);
    	SpinnerCOMA=(Spinner)findViewById(R.id.SpinnerCOMA);
    	SpinnerBaudRateCOMA=(Spinner)findViewById(R.id.SpinnerBaudRateCOMA);
    	radioButtonTxt=(RadioButton)findViewById(R.id.radioButtonTxt);
    	radioButtonHex=(RadioButton)findViewById(R.id.radioButtonHex);
    	editTextCOMA.setOnEditorActionListener(new EditorActionEvent());
		editTextTimeCOMA.setOnEditorActionListener(new EditorActionEvent());
		editTextCOMA.setOnFocusChangeListener(new FocusChangeEvent());
		editTextTimeCOMA.setOnFocusChangeListener(new FocusChangeEvent());

    	radioButtonTxt.setOnClickListener(new radioButtonClickEvent());
    	radioButtonHex.setOnClickListener(new radioButtonClickEvent());
    	ButtonClear.setOnClickListener(new ButtonClickEvent());
    	ButtonSendCOMA.setOnClickListener(new ButtonClickEvent());
    	///////////////////////////////////////////////////////////////////////
    	leftButton.setOnClickListener(new ButtonClickEvent());
    	rightButton.setOnClickListener(new ButtonClickEvent());
    	spinButton.setOnClickListener(new ButtonClickEvent());
    	forwardButton.setOnClickListener(new ButtonClickEvent());
    	backwardButton.setOnClickListener(new ButtonClickEvent());
    	
    	///////////////////////////////////////////////////////////////////////
    	toggleButtonCOMA.setOnCheckedChangeListener(new ToggleButtonCheckedChangeEvent());
    	checkBoxAutoCOMA.setOnCheckedChangeListener(new CheckBoxChangeEvent());
    	ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, 
    			R.array.baudrates_value,android.R.layout.simple_spinner_item);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	SpinnerBaudRateCOMA.setAdapter(adapter);
    	SpinnerBaudRateCOMA.setSelection(12);
    	mSerialPortFinder= new SerialPortFinder();
    	String[] entryValues = mSerialPortFinder.getAllDevicesPath();
    	List<String> allDevices = new ArrayList<String>();
		for (int i = 0; i < entryValues.length; i++) {
			allDevices.add(entryValues[i]);
		}
		ArrayAdapter<String> aspnDevices = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, allDevices);
		aspnDevices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		SpinnerCOMA.setAdapter(aspnDevices);
		if (allDevices.size()>0)
		{
			SpinnerCOMA.setSelection(0);
		}
		SpinnerCOMA.setOnItemSelectedListener(new ItemSelectedEvent());
		SpinnerBaudRateCOMA.setOnItemSelectedListener(new ItemSelectedEvent());
		DispAssistData(AssistData);
	}
    //----------------------------------------------------���ںŻ����ʱ仯ʱ���رմ򿪵Ĵ���
    class ItemSelectedEvent implements Spinner.OnItemSelectedListener{
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
		{
			if ((arg0 == SpinnerCOMA) || (arg0 == SpinnerBaudRateCOMA))
			{
				CloseComPort(ComA);
				checkBoxAutoCOMA.setChecked(false);
				toggleButtonCOMA.setChecked(false);
			}
		}

		public void onNothingSelected(AdapterView<?> arg0)
		{}
    	
    }
    //----------------------------------------------------�༭�򽹵�ת���¼�
    class FocusChangeEvent implements EditText.OnFocusChangeListener{
		public void onFocusChange(View v, boolean hasFocus)
		{
			if (v==editTextCOMA)
			{
				setSendData(editTextCOMA);
			} else if (v==editTextTimeCOMA)
			{
				setDelayTime(editTextTimeCOMA);
			}
		}
    }
    //----------------------------------------------------�༭������¼�
    class EditorActionEvent implements EditText.OnEditorActionListener{
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
		{
			if (v==editTextCOMA)
			{
				setSendData(editTextCOMA);
			} else if (v==editTextTimeCOMA)
			{
				setDelayTime(editTextTimeCOMA);
			}
			return false;
		}
    }
    //----------------------------------------------------Txt��Hexģʽѡ��
    class radioButtonClickEvent implements RadioButton.OnClickListener{
		public void onClick(View v)
		{
			if (v==radioButtonTxt)
			{
				KeyListener TxtkeyListener = new TextKeyListener(Capitalize.NONE, false);
				editTextCOMA.setKeyListener(TxtkeyListener);
				AssistData.setTxtMode(true);
			}else if (v==radioButtonHex) {
				KeyListener HexkeyListener = new NumberKeyListener()
				{
					public int getInputType()
					{
						return InputType.TYPE_CLASS_TEXT;
					}
					@Override
					protected char[] getAcceptedChars()
					{
						return new char[]{'0','1','2','3','4','5','6','7','8','9',
								'a','b','c','d','e','f','A','B','C','D','E','F'};
					}
				};
				editTextCOMA.setKeyListener(HexkeyListener);
				AssistData.setTxtMode(false);
			}
			editTextCOMA.setText(AssistData.getSendA());
			setSendData(editTextCOMA);
		}
    }
    //----------------------------------------------------�Զ�����
    class CheckBoxChangeEvent implements CheckBox.OnCheckedChangeListener{
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			if (buttonView == checkBoxAutoCOMA){
				if (!toggleButtonCOMA.isChecked() && isChecked)
				{
					buttonView.setChecked(false);
					return;
				}
				SetLoopData(ComA,editTextCOMA.getText().toString());
				SetAutoSend(ComA,isChecked);
			} 
		}
    }
    //----------------------------------------------------�����ť�����Ͱ�ť
    class ButtonClickEvent implements View.OnClickListener {
		public void onClick(View v)
		{
			if (v == ButtonClear){
				editTextRecDisp.setText("");
			} else if (v== ButtonSendCOMA){
				sendPortData(ComA, editTextCOMA.getText().toString());
			}else if(v==leftButton){
				sendPortData(ComA, "FF10FF10");
			}else if(v==rightButton){
				sendPortData(ComA, "FF10FF10");
			}else if(v==spinButton){
				sendPortData(ComA, "FF10FF10");
			}else if(v==forwardButton){
				sendPortData(ComA, "FF10FF10");
			}else if(v==backwardButton){
				sendPortData(ComA, "FF10FF10");
			}
			
		}
    }
    //----------------------------------------------------�򿪹رմ���
    class ToggleButtonCheckedChangeEvent implements ToggleButton.OnCheckedChangeListener{
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			if (buttonView == toggleButtonCOMA){
				if (isChecked){					
//						ComA=new SerialControl("/dev/s3c2410_serial0", "9600");
						ComA.setPort(SpinnerCOMA.getSelectedItem().toString());
						ComA.setBaudRate(SpinnerBaudRateCOMA.getSelectedItem().toString());
						OpenComPort(ComA);		
				}else {
					CloseComPort(ComA);
					checkBoxAutoCOMA.setChecked(false);
				}
			}
		}
    }
    //----------------------------------------------------���ڿ�����
    private class SerialControl extends SerialHelper{

//		public SerialControl(String sPort, String sBaudRate){
//			super(sPort, sBaudRate);
//		}
		public SerialControl(){
		}

		@Override
		protected void onDataReceived(final ComBean ComRecData)
		{
			//���ݽ�����������ʱ��������̣�����Ῠ��,���ܺ�6410����ʾ�����й�
			//ֱ��ˢ����ʾ��������������ʱ���������ԣ�����������ʾͬ����
			//���̶߳�ʱˢ����ʾ���Ի�ý���������ʾЧ�������ǽ��������ٶȿ�����ʾ�ٶ�ʱ����ʾ���ͺ�
			//����Ч�����-_-���̶߳�ʱˢ���Ժ�һЩ��
			DispQueue.AddQueue(ComRecData);//�̶߳�ʱˢ����ʾ(�Ƽ�)
			/*
			runOnUiThread(new Runnable()//ֱ��ˢ����ʾ
			{
				public void run()
				{
					DispRecData(ComRecData);
				}
			});*/
		}
    }
    //----------------------------------------------------ˢ����ʾ�߳�
    private class DispQueueThread extends Thread{
		private Queue<ComBean> QueueList = new LinkedList<ComBean>(); 
		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				final ComBean ComData;
		        while((ComData=QueueList.poll())!=null)
		        {
		        	runOnUiThread(new Runnable()
					{
						public void run()
						{
							DispRecData(ComData);
						}
					});
		        	try
					{
		        		Thread.sleep(100);//��ʾ���ܸߵĻ������԰Ѵ���ֵ��С��
					} catch (Exception e)
					{
						e.printStackTrace();
					}
		        	break;
				}
			}
		}

		public synchronized void AddQueue(ComBean ComData){
			QueueList.add(ComData);
		}
	}
    //----------------------------------------------------ˢ�½�������
    private void DispAssistData(AssistBean AssistData)
	{
    	editTextCOMA.setText(AssistData.getSendA());
    	setSendData(editTextCOMA);
    	if (AssistData.isTxt())
		{
			radioButtonTxt.setChecked(true);
		} else
		{
			radioButtonHex.setChecked(true);
		}
    	editTextTimeCOMA.setText(AssistData.sTimeA);
    	setDelayTime(editTextTimeCOMA);
	}
    //----------------------------------------------------���桢��ȡ��������
    private void saveAssistData(AssistBean AssistData) { 
    	AssistData.sTimeA = editTextTimeCOMA.getText().toString();
    	SharedPreferences msharedPreferences = getSharedPreferences("ComAssistant", Context.MODE_PRIVATE);
        try {  
            ByteArrayOutputStream baos = new ByteArrayOutputStream();  
            ObjectOutputStream oos = new ObjectOutputStream(baos);  
            oos.writeObject(AssistData); 
            String sBase64 = new String(Base64.encode(baos.toByteArray(),0)); 
            SharedPreferences.Editor editor = msharedPreferences.edit();  
            editor.putString("AssistData", sBase64);  
            editor.commit();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
    //----------------------------------------------------
    private AssistBean getAssistData() {  
    	SharedPreferences msharedPreferences = getSharedPreferences("ComAssistant", Context.MODE_PRIVATE);
    	AssistBean AssistData =	new AssistBean();
        try {  
            String personBase64 = msharedPreferences.getString("AssistData", "");  
            byte[] base64Bytes = Base64.decode(personBase64.getBytes(),0);  
            ByteArrayInputStream bais = new ByteArrayInputStream(base64Bytes);  
            ObjectInputStream ois = new ObjectInputStream(bais);  
            AssistData = (AssistBean) ois.readObject();
            return AssistData;
        } catch (Exception e) {  
            e.printStackTrace();  
        }
		return AssistData;  
    }  
    //----------------------------------------------------�����Զ�������ʱ
    private void setDelayTime(TextView v){
    	if (v==editTextTimeCOMA)
		{
			AssistData.sTimeA = v.getText().toString();
			SetiDelayTime(ComA, v.getText().toString());
		}
    }
    //----------------------------------------------------�����Զ���������
    private void setSendData(TextView v){
    	if (v==editTextCOMA)
		{
			AssistData.setSendA(v.getText().toString());
			SetLoopData(ComA, v.getText().toString());
		} 
    }
    //----------------------------------------------------�����Զ�������ʱ
    private void SetiDelayTime(SerialHelper ComPort,String sTime){
    	ComPort.setiDelay(Integer.parseInt(sTime));
    }
    //----------------------------------------------------�����Զ���������
    private void SetLoopData(SerialHelper ComPort,String sLoopData){
    	if (radioButtonTxt.isChecked())
		{
			ComPort.setTxtLoopData(sLoopData);
		} else if (radioButtonHex.isChecked())
		{
			ComPort.setHexLoopData(sLoopData);
		}
    }
    //----------------------------------------------------��ʾ��������
    private void DispRecData(ComBean ComRecData){
    	StringBuilder sMsg=new StringBuilder();
    	sMsg.append(ComRecData.sRecTime);
    	sMsg.append("[");
    	sMsg.append(ComRecData.sComPort);
    	sMsg.append("]");
    	if (radioButtonTxt.isChecked())
		{
			sMsg.append("[Txt] ");
			sMsg.append(new String(ComRecData.bRec));
		}else if (radioButtonHex.isChecked()) {
			sMsg.append("[Hex] ");
			sMsg.append(MyFunc.ByteArrToHex(ComRecData.bRec));
		}
    	sMsg.append("\r\n");
    	editTextRecDisp.append(sMsg);
    	iRecLines++;
    	editTextLines.setText(String.valueOf(iRecLines));
    	if ((iRecLines > 500) && (checkBoxAutoClear.isChecked()))//�ﵽ500���Զ����
		{
    		editTextRecDisp.setText("");
    		editTextLines.setText("0");
    		iRecLines=0;
		}
    }
    //----------------------------------------------------�����Զ�����ģʽ����
    private void SetAutoSend(SerialHelper ComPort,boolean isAutoSend){
    	if (isAutoSend)
		{
    		ComPort.startSend();
		} else
		{
			ComPort.stopSend();
		}
    }
    //----------------------------------------------------���ڷ���
    private void sendPortData(SerialHelper ComPort,String sOut){
    	if (ComPort!=null && ComPort.isOpen())
		{
    		if (radioButtonTxt.isChecked())
			{
				ComPort.sendTxt(sOut);
			}else if (radioButtonHex.isChecked()) {
				ComPort.sendHex(sOut);
			}
		}
    }
    //----------------------------------------------------�رմ���
    private void CloseComPort(SerialHelper ComPort){
    	if (ComPort!=null){
    		ComPort.stopSend();
    		ComPort.close();
		}
    }
    //----------------------------------------------------�򿪴���
    private void OpenComPort(SerialHelper ComPort){
    	try
		{
			ComPort.open();
		} catch (SecurityException e) {
			ShowMessage("�򿪴���ʧ��:û�д��ڶ�/дȨ��!");
		} catch (IOException e) {
			ShowMessage("�򿪴���ʧ��:δ֪����!");
		} catch (InvalidParameterException e) {
			ShowMessage("�򿪴���ʧ��:��������!");
		}
    }
    //------------------------------------------��ʾ��Ϣ
  	private void ShowMessage(String sMsg)
  	{
  		Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
  	}
}