package com.github.pwittchen.neurosky.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.github.pwittchen.neurosky.library.NeuroSky;
import com.github.pwittchen.neurosky.library.exception.BluetoothNotEnabledException;
import com.github.pwittchen.neurosky.library.listener.ExtendedDeviceMessageListener;
import com.github.pwittchen.neurosky.library.message.enums.BrainWave;
import com.github.pwittchen.neurosky.library.message.enums.Signal;
import com.github.pwittchen.neurosky.library.message.enums.State;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

  private final static String LOG_TAG = "NeuroSky";
  private NeuroSky neuroSky;
  private int nAttention;
  private int nMeditation;

  private int cIdx = 1;
  private int nTestNum = 1;
  private int[] aAttention;
  private int[] aMeditation;
  private String cFileCSV;
  private String cFileRaw;

  boolean bMode;
  boolean bSave;
  Switch btnConnect;
  Button btnTestStart;
  Button btnTestStop;
  Button btnTaskStart;
  Button btnTaskStop;
  EditText txtFIO;
  EditText txtAge;
  EditText txtMarks;
  EditText txtProf;

  @BindView(R.id.tv_state) TextView tvState;
  @BindView(R.id.tv_attention) TextView tvAttention;
  @BindView(R.id.tv_meditation) TextView tvMeditation;
  @BindView(R.id.txtTask) TextView txtTask;
  @BindView(R.id.txtFileName) TextView txtFileName;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE},
            PackageManager.PERMISSION_GRANTED
    );

    ButterKnife.bind(this);
    neuroSky = createNeuroSky();

    // Switcher
    btnConnect = findViewById(R.id.btnConnect);
    btnConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) {
          // try to connect
          try {
            neuroSky.connect();
            neuroSky.start();
            tvState.setText("Гарнитура подключена");
          } catch (BluetoothNotEnabledException e) {
            btnConnect.setChecked(false);
            tvState.setText("Включите Bluetooth");
          }
        } else {
          // disconnect
          neuroSky.stop();
          neuroSky.disconnect();
          tvState.setText("Подключите гарнитуру...");
          tvAttention.setText("Внимание: нет данных");
          tvMeditation.setText("Медитация: нет данных");
        }
        btnTestStart.setEnabled(btnConnect.isChecked());
      }
    });

    //init
    cIdx = 1;
    bMode = false;
    bSave = false;
    aAttention = new int[256];
    aMeditation = new int[256];
    btnTestStart = findViewById(R.id.btnTestStart);
    btnTestStop = findViewById(R.id.btnTestStop);
    btnTaskStart = findViewById(R.id.btnTaskStart);
    btnTaskStop = findViewById(R.id.btnTaskStop);
    txtFIO = findViewById(R.id.txtFIO);
    txtAge = findViewById(R.id.txtAge);
    txtMarks = findViewById(R.id.txtMarks);
    txtProf = findViewById(R.id.txtProf);

    // set enables
    setButtonsEnable();
    btnTestStart.setEnabled(btnConnect.isChecked());
    btnTestStart.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Start test button
       if (!btnConnect.isChecked()) {
          Toast.makeText(MainActivity.this, "Подключите гарнитуру!", Toast.LENGTH_LONG).show();
          return;
        }
        if (txtFIO.getText().toString().trim().equals("")) {
          Toast.makeText(MainActivity.this, "Укажите имя!", Toast.LENGTH_LONG).show();
          return;
        }
        if (txtAge.getText().toString().trim().equals("")) {
          Toast.makeText(MainActivity.this, "Укажите возраст!", Toast.LENGTH_LONG).show();
          return;
        }
        if (txtProf.getText().toString().trim().equals("")) {
          Toast.makeText(MainActivity.this, "Укажите род деятельности!", Toast.LENGTH_LONG).show();
          return;
        }
        nTestNum = 1;
        bMode = true;
        bSave = false;
        setButtonsEnable();
        getFileNames();
        txtFileName.setText("Имя файла: " + cFileCSV);
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
          Toast.makeText(MainActivity.this, "SD карта не доступна!", Toast.LENGTH_LONG).show();
          return;
        }
        File sdPath = Environment.getExternalStorageDirectory();
        sdPath = new File(sdPath.getAbsolutePath() + "/MindWave/");
        // get current time
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        String dateText = dateFormat.format(currentDate);

        File sdFileRaw = new File(sdPath, cFileRaw);
        try {
          BufferedWriter bw = new BufferedWriter(new FileWriter(sdFileRaw, true));
          bw.write("Name;" + txtFIO.getText().toString());
          bw.write("\n");
          bw.write("Age;" + txtAge.getText().toString());
          bw.write("\n");
          bw.write("Profession;" + txtProf.getText().toString());
          bw.write("\n");
          bw.write("Start time;" + dateText);
          bw.write("\n");
          bw.write("\n");
          bw.write("Number;Time;Attention;Meditation");
          bw.write("\n");
          bw.close();
        } catch (IOException e) {
          return;
        }
        File sdFileCSV = new File(sdPath, cFileCSV);
        try {
          BufferedWriter bw = new BufferedWriter(new FileWriter(sdFileCSV, true));
          bw.write("Name;" + txtFIO.getText().toString());
          bw.write("\n");
          bw.write("Age;" + txtAge.getText().toString());
          bw.write("\n");
          bw.write("Profession;" + txtProf.getText().toString());
          bw.write("\n");
          bw.write("Start time;" + dateText);
          bw.write("\n");
          bw.write("\n");
          bw.write("Number;Time;Attention;Meditation");
          bw.write("\n");
          bw.close();
        } catch (IOException e) {
          return;
        }
      }
    });
    btnTestStop.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Stop test button
        nTestNum = 1;
        bMode = false;
        bSave = false;
        setButtonsEnable();
        File sdPath = Environment.getExternalStorageDirectory();
        sdPath = new File(sdPath.getAbsolutePath() + "/MindWave/");
        // get current time
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        String dateText = dateFormat.format(currentDate);
        File sdFileRaw = new File(sdPath, cFileRaw);
        try {
          BufferedWriter bw = new BufferedWriter(new FileWriter(sdFileRaw, true));
          bw.write("\n");
          bw.write("Finish time;" + dateText);
          bw.write("\n");
          bw.write("Marks;" + txtMarks.getText().toString());
          bw.close();
        } catch (IOException e) {
          return;
        }
        File sdFileCSV = new File(sdPath, cFileCSV);
        try {
          BufferedWriter bw = new BufferedWriter(new FileWriter(sdFileCSV, true));
          bw.write("\n");
          bw.write("Finish time;" + dateText);
          bw.write("\n");
          bw.write("\n");
          bw.write("Marks;" + txtMarks.getText().toString());
          bw.close();
        } catch (IOException e) {
          return;
        }
      }
    });
    btnTaskStart.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        int nIdx;
        // Start task button
        bSave = true;
        setButtonsEnable();
        cIdx = 1;
        nIdx = 1;
        while (nIdx <= 255) {
          aAttention[nIdx] = 0;
          aMeditation[nIdx] = 0;
          nIdx++;
        }
      }
    });
    btnTaskStop.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Stop task button
        bSave = false;
        setButtonsEnable();
        saveToFile();
        nTestNum++;
      }
    });
  }

  void saveToFile() {
    int nIdx;
    int nSum;
    int nAvgAtt = 0;
    int nAvgMed = 0;
    // save rawdata to file
    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
      Toast.makeText(MainActivity.this, "SD карта не доступна!", Toast.LENGTH_LONG).show();
      return;
    }
    File sdPath = Environment.getExternalStorageDirectory();
    sdPath = new File(sdPath.getAbsolutePath() + "/MindWave/");
    File sdFileRaw = new File(sdPath, cFileRaw);
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(sdFileRaw, true));
      bw.write(Integer.toString(nTestNum));
      bw.write(";");
      bw.write(Integer.toString(cIdx-1));
      bw.write(";");
      nIdx = 1;
      nSum = 0;
      //process attention (with calc average data)
      while (nIdx < cIdx) {
        bw.write(Integer.toString(aAttention[nIdx]));
        if (nIdx < cIdx-1) {
          bw.write(",");
        } else {
          bw.write(";");
        }
        nSum = nSum + aAttention[nIdx];
        nIdx++;
      }
      nAvgAtt = nSum / (cIdx-1);
      //process meditation (with calc average data)
      nIdx = 1;
      nSum = 0;
      while (nIdx < cIdx) {
        bw.write(Integer.toString(aMeditation[nIdx]));
        if (nIdx < cIdx-1) {
          bw.write(",");
        } else {
          bw.write(";");
        }
        nSum = nSum + aMeditation[nIdx];
        nIdx++;
      }
      nAvgMed = nSum / (cIdx-1);
      bw.write("\n");
      bw.close();
    } catch (IOException e) {
      return;
    }
    //save CSV
    File sdFileCSV = new File(sdPath, cFileCSV);
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(sdFileCSV, true));
      bw.write(Integer.toString(nTestNum));
      bw.write(";");
      bw.write(Integer.toString(cIdx-1));
      bw.write(";");
      bw.write(Integer.toString(nAvgAtt));
      bw.write(";");
      bw.write(Integer.toString(nAvgMed));
      bw.write(";");
      bw.write("\n");
      bw.close();
    } catch (IOException e) {
      return;
    }
  }

  void updateConfig(int nFile){
    // проверяем доступность SD
    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
      Toast.makeText(MainActivity.this, "SD карта не доступна!", Toast.LENGTH_LONG).show();
      return;
    }
    File sdPath = Environment.getExternalStorageDirectory();
    sdPath = new File(sdPath.getAbsolutePath() + "/MindWave/");
    sdPath.mkdirs();
    File sdFile = new File(sdPath, "config.ini");
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile));
      bw.write(nFile);
      bw.close();
    } catch (IOException e) {
      return;
    }
  }
  void getFileNames() {
    int nFile = 0;
    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
      Toast.makeText(MainActivity.this, "SD карта не доступна!", Toast.LENGTH_LONG).show();
      return;
    }
    File sdPath = Environment.getExternalStorageDirectory();
    sdPath = new File(sdPath.getAbsolutePath() + "/MindWave/");
    File sdFile = new File(sdPath, "config.ini");
    try (InputStream inputStream = new FileInputStream(sdFile);) {
      nFile = inputStream.read();
    } catch (IOException e) {
      updateConfig(1);
    }
    nFile++;
    updateConfig(nFile);
    cFileCSV = "test_"+nFile+".csv";
    cFileRaw = "test_"+nFile+"_raw.csv";
  }

  void setButtonsEnable() {
      btnTestStart.setEnabled(!bMode);
      btnTestStop.setEnabled(bMode && !bSave);
      btnTaskStart.setEnabled(bMode && !bSave);
      btnTaskStop.setEnabled(bMode && bSave);
  }

  @NonNull private NeuroSky createNeuroSky() {
    return new NeuroSky(new ExtendedDeviceMessageListener() {
      @Override public void onStateChange(State state) {
        handleStateChange(state);
      }

      @Override public void onSignalChange(Signal signal) {
        handleSignalChange(signal);
      }

      @Override public void onBrainWavesChange(Set<BrainWave> brainWaves) {
        handleBrainWavesChange(brainWaves);
      }
    });
  }

  private void handleStateChange(final State state) {
    String stateLocal = "";
    if (neuroSky != null && state.equals(State.CONNECTED)) {
      neuroSky.start();
    }
    switch (state.toString()) {
      case "IDLE":
        stateLocal = "Отключено";
        break;
      case "CONNECTING":
        stateLocal = "Подключение...";
        break;
      case "NOT_FOUND":
        stateLocal = "Гарнитура не найдена";
        break;
      case "CONNECTED":
        stateLocal = "Гарнитура подключена";
        break;
    }
    tvState.setText(stateLocal.toString());
  }

  private void handleSignalChange(final Signal signal) {
    switch (signal) {
      case ATTENTION:
        nAttention = signal.getValue();
        tvAttention.setText(getFormattedMessage("Внимание: %d", signal));
        break;
      case MEDITATION:
        nMeditation = signal.getValue();
        tvMeditation.setText(getFormattedMessage("Медитация: %d", signal));
        // save current value
        if (bSave) {
          txtTask.setText("Задание: " + nTestNum + " / " + cIdx);
          if (cIdx <= 255) {
            aAttention[cIdx] = nAttention;
            aMeditation[cIdx] = nMeditation;
            cIdx++;
          }
        }
        if (bMode && !bSave) {
          txtTask.setText("Задание: " + nTestNum);
        }
        break;
    }
  }

  private String getFormattedMessage(String messageFormat, Signal signal) {
    return String.format(Locale.getDefault(), messageFormat, signal.getValue());
  }

  private void handleBrainWavesChange(final Set<BrainWave> brainWaves) {
    for (BrainWave brainWave : brainWaves) {
      Log.d(LOG_TAG, String.format("%s: %d", brainWave.toString(), brainWave.getValue()));
    }
  }

}
