package com.softmobile.Bingo.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.softmobile.Bingo.R;
import com.softmobile.Bingo.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding m_binding = null;
    private ArrayList<BingoButton> m_alBingoButton = new ArrayList<>();
    private Random m_rRandom = new Random();
    private String m_strPreText = "";
    private boolean m_bUserTyped = false;
    private int m_iColor = 0;
    private int m_iMode = 1;//0是遊戲模式，1是輸入模式
    private int m_iRangeMin = 0;
    private int m_iRangeMax = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = m_binding.getRoot();
        setContentView(view);

        m_iColor = getResources().getColor(R.color.purple_500);

        //判斷當前模式為何
        m_binding.switchMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (m_binding.switchMode.isChecked()) {
                    //變為輸入模式
                    m_iMode = 1;
                    m_binding.etNumRange.setFocusableInTouchMode(true);
                    m_binding.etNumRange.setFocusable(true);
                    m_binding.etRowRange.setFocusableInTouchMode(true);
                    m_binding.etRowRange.setFocusable(true);
                    for (int i = 0; i < m_alBingoButton.size(); i++) {
                        EditText etButton = m_alBingoButton.get(i).getEditTextButton();
                        etButton.setFocusable(true);
                        etButton.setFocusableInTouchMode(true);
                    }
                } else if (!m_binding.switchMode.isChecked()) {
                    //變為遊戲模式
                    m_iMode = 0;
                    m_binding.etNumRange.setFocusableInTouchMode(false);
                    m_binding.etNumRange.setFocusable(false);
                    m_binding.etRowRange.setFocusableInTouchMode(false);
                    m_binding.etRowRange.setFocusable(false);
                    for (int i = 0; i < m_alBingoButton.size(); i++) {
                        EditText etButton = m_alBingoButton.get(i).getEditTextButton();
                        etButton.setFocusable(false);
                        etButton.setFocusableInTouchMode(false);
                    }
                }
            }
        });

        //產生亂數
        m_binding.btnRandom.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                ArrayList<Integer> alRandomNumbers = new ArrayList<>();
                int iRandomNumber = 0;
                if (m_iMode == 1) {
                    if (Pattern.matches("\\d+-\\d+", m_binding.etNumRange.getText())) {
                        String randomRange = m_binding.etNumRange.getText().toString();
                        String[] rangeMinMax = randomRange.split("-");
                        m_iRangeMin = Integer.parseInt(rangeMinMax[0]);
                        m_iRangeMax = Integer.parseInt(rangeMinMax[1]);
                        if ((m_iRangeMax - m_iRangeMin + 1) >= m_alBingoButton.size()) {
                            for (int i = 0; i < m_alBingoButton.size(); i++) {
                                do {
                                    iRandomNumber = m_rRandom.nextInt(
                                            m_iRangeMax - m_iRangeMin + 1) + m_iRangeMin;
                                } while (alRandomNumbers.contains(iRandomNumber));
                                alRandomNumbers.add(iRandomNumber);

                                m_alBingoButton.get(i).getEditTextButton().setText(iRandomNumber + "");
                                m_alBingoButton.get(i).setButtonNum(iRandomNumber);
                            }
                        } else {
                            //toast格式錯誤訊息
                            Toast.makeText(MainActivity.this,
                                    R.string.inputNumError,
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                R.string.inputNumError,
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this,
                            R.string.modeError,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //改變賓果的行列數量
        m_binding.ivRowsCheck.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onClick(View view) {
                int iRowNum = Integer.parseInt(m_binding.etRowRange.getText().toString());
                if (m_iMode == 1) {
                    if (iRowNum >= 2 && iRowNum <= 5) {
                        if (m_binding.bingoTable.getChildCount() != 0) {
                            m_alBingoButton.clear();
                            m_binding.bingoTable.removeAllViews();
                        }
                        for (int i = 0; i < iRowNum; i++) {
                            TableRow row = new TableRow(MainActivity.this);
                            row.setGravity(Gravity.CENTER_HORIZONTAL);
                            for (int j = 0; j < iRowNum; j++) {
                                Button button = new Button(MainActivity.this);
                                EditText etButton = new EditText(MainActivity.this);
                                BingoButton bingoButton = new BingoButton(button, etButton, i * iRowNum + j);

                                //row.addView(button);
                                row.addView(etButton);
                                m_alBingoButton.add(bingoButton);

                                //設定按鈕外觀
                                /*button.setText("");
                                button.setOnClickListener(bingoOnClickListener);
                                button.setBackgroundResource(R.drawable.button_style);
                                button.setBackgroundColor(m_iColor);
                                button.getLayoutParams().width = (m_binding.bingoTable.getWidth() / iRowNum);
                                button.getLayoutParams().height = (m_binding.bingoTable.getHeight() / iRowNum);*/

                                //設定editText
                                etButton.setText("");
                                etButton.setTextSize(20);
                                etButton.setBackgroundResource(R.drawable.button_style);
                                etButton.setOnClickListener(bingoOnClickListener);
                                etButton.setGravity(Gravity.CENTER);
                                etButton.setTextColor(Color.WHITE);
                                etButton.setBackgroundColor(m_iColor);
                                etButton.getLayoutParams().width = (m_binding.bingoTable.getWidth() / iRowNum);
                                etButton.getLayoutParams().height = (m_binding.bingoTable.getHeight() / iRowNum);                                /*etButton.addTextChangedListener(new TextWatcher() {
                                    @Override
                                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                        m_strPreText = charSequence.toString();
                                    }

                                    @Override
                                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                        if(m_bUserTyped){
                                        for (int j = 0; j < m_alBingoButton.size(); j++) {
                                            String editTextText = charSequence.toString();
                                            String buttonText = m_alBingoButton.get(j).getEditText().getText().toString();

                                            if (editTextText.equals(buttonText)) {
                                                etButton.setText(m_strPreText);
                                                break;
                                            }
                                        }}
                                    }

                                    @Override
                                    public void afterTextChanged(Editable editable) {
                                    }
                                });
                                etButton.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        m_bUserTyped = true;
                                        return false;
                                    }
                                });
                                m_bUserTyped = false;*/
                            }
                            m_binding.bingoTable.addView(row, i);
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this,
                            R.string.modeError,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //radioGroup換顏色
        m_binding.rgChangeColor.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.rbtnRed) {
                    m_iColor = getResources().getColor(R.color.red);
                } else if (i == R.id.rbtnOrange) {
                    m_iColor = getResources().getColor(R.color.orange);
                } else if (i == R.id.rbtnGreen) {
                    m_iColor = getResources().getColor(R.color.green);
                } else if (i == R.id.rbtnPurple) {
                    m_iColor = getResources().getColor(R.color.purple_500);
                } else {
                }
                for (int j = 0; j < m_alBingoButton.size(); j++) {
                    m_alBingoButton.get(j).getEditTextButton().setBackgroundColor(m_iColor);
                }
            }
        });
    }

    //賓果按鈕換顏色
    private EditText.OnClickListener bingoOnClickListener = new EditText.OnClickListener() {
        @SuppressLint("ResourceAsColor")
        @Override
        public void onClick(View v) {
            if (m_iMode == 0) {
                if (!m_alBingoButton.get((Integer) v.getTag()).getButtonClicked()) {
                    m_alBingoButton.get((Integer) v.getTag()).setButtonClicked(true);
                    v.setBackgroundColor(R.color.white);
                } else {
                    m_alBingoButton.get((Integer) v.getTag()).setButtonClicked(false);
                    v.setBackgroundColor(m_iColor);
                }
            }
        }
    };

    private void checkBingoConnection() {
        boolean[] rowConnected = new boolean[m_binding.bingoTable.getChildCount()];
        boolean[] colConnected = new boolean[m_binding.bingoTable.getChildCount()];
        boolean diag1Connected = true;
        boolean diag2Connected = true;
        int connectedCount = 0;

        for (int i = 0; i < m_binding.bingoTable.getChildCount(); i++) {
            for (int j = 0; j < m_binding.bingoTable.getChildCount(); j++) {
                BingoButton bingoButton = m_alBingoButton.get(i * m_binding.bingoTable.getChildCount() + j);
                if (bingoButton.getButtonClicked()) {
                    rowConnected[i] = true;
                    colConnected[j] = true;
                    if (i == j) {
                        diag1Connected = diag1Connected && true;
                    }
                    if (i + j == m_binding.bingoTable.getChildCount() - 1) {
                        diag2Connected = diag2Connected && true;
                    }
                }
            }
        }
        for (boolean row : rowConnected) {
            if (row) {
                connectedCount++;
            }
        }
        for (boolean col : colConnected) {
            if (col) {
                connectedCount++;
            }
        }
        if (diag1Connected) {
            connectedCount++;
        }
        if (diag2Connected) {
            connectedCount++;
        }

        if (connectedCount >= 3) {
            Toast.makeText(MainActivity.this, "游戏结束！", Toast.LENGTH_SHORT).show();
        }
    }


    public void checkBingoLine() {
        for (int i = 0; i < m_alBingoButton.size(); i++) {
            if (checkLine(i, i + 1, i + 2)) {

            }
        }
    }

    public boolean checkLine(int a, int b, int c) {
        return m_alBingoButton.get(a).getButtonClicked() &&
                m_alBingoButton.get(b).getButtonClicked() &&
                m_alBingoButton.get(c).getButtonClicked();
    }

}
