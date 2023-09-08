package com.softmobile.Bingo.ui;

import static com.google.android.material.internal.ViewUtils.hideKeyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.icu.text.MessagePattern;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
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

import java.security.cert.PKIXRevocationChecker;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding m_binding = null;
    private ArrayList<BingoButton> m_alBingoButton = new ArrayList<>();
    private final Random m_rRandom = new Random();
    private String m_strPreNum = "";
    private int m_iBingoLines = 0;
    private int m_iRows = 0;
    private int m_iColor = 0;
    private int m_iMode = 1;//0是遊戲模式，1是輸入模式
    private int m_iRangeMin = 0;
    private int m_iRangeMax = 0;
    private View vBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_binding = ActivityMainBinding.inflate(getLayoutInflater());
        vBinding = m_binding.getRoot();
        setContentView(vBinding);

        m_iColor = getResources().getColor(R.color.purple_500);

        //判斷當前模式為何
        m_binding.switchMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (m_binding.switchMode.isChecked()) {
                    //變為輸入模式
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("切換至輸入模式");
                    builder.setMessage("確定後Bingo將被復原！");
                    builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            m_iMode = 1;
                            m_iBingoLines = 0;
                            modeSettings();
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            m_binding.switchMode.setChecked(false);
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } else if (!m_binding.switchMode.isChecked()) {
                    //變為遊戲模式
                    m_iMode = 0;
                    modeSettings();
                }
            }
        });

        //偵測數字範圍改變
        m_binding.etNumRange.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                //按下確認鍵或是(鍵盤事件不為null,鍵盤事件為按下,按下鍵為ENTER)
                if (i == EditorInfo.IME_ACTION_DONE ||
                        (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                                keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    checkNumRange();
                    hideKeyboard(MainActivity.this, vBinding);
                    return true;
                }
                return false;
            }
        });

        //偵測行數改變
        m_binding.etRowRange.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                //按下確認鍵或是(鍵盤事件不為null,鍵盤事件為按下,按下鍵為ENTER)
                if (i == EditorInfo.IME_ACTION_DONE ||
                        (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                                keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (!TextUtils.isEmpty(m_binding.etRowRange.getText())) {
                        int enteredNum = Integer.parseInt(m_binding.etRowRange.getText().toString());
                        if (enteredNum != 0) {
                            if (enteredNum >= 3 && enteredNum <= 5) {
                                m_iRows = enteredNum;
                            } else {
                                toastMsg(R.string.inputNumError);
                            }
                        } else {
                            toastMsg(R.string.zeroNum);
                        }
                    } else {
                        toastMsg(R.string.inputEmpty);
                    }
                    hideKeyboard(MainActivity.this, vBinding);
                    return true;
                }
                return false;
            }
        });

        //產生亂數
        m_binding.btnRandom.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                hideKeyboard(MainActivity.this, vBinding);
                ArrayList<Integer> alRandomNumbers = new ArrayList<>();//set
                int iRandomNumber = 0;
                if (checkNumRange()) {
                    for (int i = 0; i < m_alBingoButton.size(); i++) {
                        do {
                            iRandomNumber = m_rRandom.nextInt(
                                    m_iRangeMax - m_iRangeMin + 1) + m_iRangeMin;
                        } while (alRandomNumbers.contains(iRandomNumber));
                        alRandomNumbers.add(iRandomNumber);

                        m_alBingoButton.get(i).getEditTextButton().setText(iRandomNumber + "");
                        m_alBingoButton.get(i).setButtonNum(iRandomNumber);
                    }
                }
            }
        });

        //改變賓果的行列數量，加入按鈕及相關設定
        m_binding.ivRowsCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(m_binding.etRowRange.getText())) {
                    int enteredNum = Integer.parseInt(m_binding.etRowRange.getText().toString());
                    if (enteredNum != 0) {
                        if (enteredNum >= 2 && enteredNum <= 5) {
                            m_iRows = enteredNum;

                            if (m_binding.bingoTable.getChildCount() != 0) {
                                m_alBingoButton.clear();
                                m_binding.bingoTable.removeAllViews();
                            }
                            for (int i = 0; i < m_iRows; i++) {
                                TableRow row = new TableRow(MainActivity.this);
                                row.setGravity(Gravity.CENTER_HORIZONTAL);
                                for (int j = 0; j < m_iRows; j++) {
                                    EditText etButton = new EditText(MainActivity.this);
                                    BingoButton bingoButton = new BingoButton(etButton, i * m_iRows + j);
                                    row.addView(etButton);
                                    m_alBingoButton.add(bingoButton);

                                    //editText的設定
                                    etButton.setText("");
                                    etButton.setId(i * m_iRows + j);
                                    etButton.setTextSize(25);
                                    etButton.setInputType(InputType.TYPE_CLASS_NUMBER);
                                    etButton.setOnClickListener(bingoOnClickListener);
                                    etButton.setOnEditorActionListener(bingoTextChangeListener);
                                    etButton.setGravity(Gravity.CENTER);
                                    etButton.setSingleLine();
                                    etButton.setImeOptions(EditorInfo.IME_ACTION_DONE);
                                    etButton.setTextColor(Color.WHITE);
                                    etButton.setBackgroundColor(m_iColor);
                                    etButton.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
                                    etButton.getLayoutParams().width = (m_binding.bingoTable.getWidth() / m_iRows);
                                    etButton.getLayoutParams().height = (m_binding.bingoTable.getHeight() / m_iRows);

                                    //在輸入新數字前先記下舊的數字，以便復原
                                    etButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                        @Override
                                        public void onFocusChange(View v, boolean hasFocus) {
                                            if (!hasFocus) {
                                                etButton.setCursorVisible(false);
                                            } else {
                                                m_strPreNum = etButton.getText() + "";
                                                etButton.setCursorVisible(true);
                                            }
                                        }
                                    });
                                }
                                m_binding.bingoTable.addView(row, i);
                            }
                        } else {
                            toastMsg(R.string.inputNumError);
                        }
                    } else {
                        toastMsg(R.string.zeroNum);
                    }
                } else {
                    toastMsg(R.string.inputEmpty);
                }
                hideKeyboard(MainActivity.this, vBinding);

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
                }
                for (int j = 0; j < m_alBingoButton.size(); j++) {
                    m_alBingoButton.get(j).setButtonClicked(false);
                    m_alBingoButton.get(j).getEditTextButton().setBackgroundColor(m_iColor);
                }
            }
        });
    }

    //偵測賓果按鈕輸入的數字是否重複或超出範圍
    private EditText.OnEditorActionListener bingoTextChangeListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView etButton, int i, KeyEvent keyEvent) {
            //按下確認鍵或是(鍵盤事件不為null,鍵盤事件為按下,按下鍵為ENTER)
            if (i == EditorInfo.IME_ACTION_DONE ||
                    (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                            keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                int iNewNum = 0;
                int iButtonNum = 0;
                String enteredText = etButton.getText().toString();
                /*if (enteredText.charAt(0) == '0') {
                    enteredText = enteredText.substring(1);
                }*/
                if (!TextUtils.isEmpty(enteredText)) {
                    iNewNum = Integer.parseInt(enteredText);
                    if (iNewNum == 0) {
                        for (int j = 0; j < m_alBingoButton.size(); j++) {
                            iButtonNum = m_alBingoButton.get(j).getButtonNum();
                            if (iNewNum == iButtonNum ||
                                    iNewNum < m_iRangeMin ||
                                    iNewNum > m_iRangeMax) {
                                etButton.setText(m_strPreNum);
                                toastMsg(R.string.numberExists);
                            }
                        }
                        m_alBingoButton.get((Integer) etButton.getTag()).setButtonNum(iNewNum);
                    } else {
                        etButton.setText(m_strPreNum);
                        toastMsg(R.string.zeroNum);
                    }
                } else {
                    etButton.setText(m_strPreNum);
                    toastMsg(R.string.inputEmpty);
                }
                hideKeyboard(MainActivity.this, vBinding);
                return true;
            }
            return false;
        }
    };

    //賓果按鈕點擊事件(換顏色,確認賓果)
    private EditText.OnClickListener bingoOnClickListener = new EditText.OnClickListener() {
        @Override
        public void onClick(View view) {
            /*
            listener寫一起的話，用以下的寫法來判斷是監聽到誰
            if (view.getId() == m_binding.etNumRange.getId()) {

            }
            */
            if (m_iMode == 0) {
                if (!m_alBingoButton.get((Integer) view.getTag()).getButtonClicked()) {
                    m_alBingoButton.get((Integer) view.getTag()).setButtonClicked(true);
                    int red = Color.red(m_iColor);
                    int green = Color.green(m_iColor);
                    int blue = Color.blue(m_iColor);
                    view.setBackgroundColor(Color.rgb(Math.max(0, red - 30),
                            Math.max(0, green - 30),
                            Math.max(0, blue - 30)));
                    checkBingoLine();
                } else {
                    m_alBingoButton.get((Integer) view.getTag()).setButtonClicked(false);
                    view.setBackgroundColor(m_iColor);
                    checkBingoLine();
                }
            }
        }
    };


    @SuppressLint("SetTextI18n")
    public void checkBingoLine() {
        m_binding.tvBingoLines.setText(getResources()
                .getString(R.string.bingoLines) + m_iBingoLines);
        //橫排判斷
        for (int i = 0; i < m_iRows; i++) {
            boolean bIsBingo = true;
            for (int j = 0; j < m_iRows; j++) {
                boolean bIsClicked = m_alBingoButton.get(i * m_iRows + j).getButtonClicked();
                //如果當前按鈕被按過就維持Bingo狀態
                if (!bIsClicked) {
                    bIsBingo = false;
                    break;
                }
            }
            //每一排結束時就判斷是否還是Bingo狀態
            if (bIsBingo) {
                m_iBingoLines++;
                m_binding.tvBingoLines.setText(getResources()
                        .getString(R.string.bingoLines) + m_iBingoLines);                //如果已Bingo就跳出迴圈
                if (checkGameOver()) {
                    break;
                }
            }
        }

        //直排判斷
        for (int i = 0; i < m_iRows; i++) {
            boolean bIsBingo = true;
            for (int j = 0; j < m_iRows; j++) {
                boolean bIsClicked = m_alBingoButton.get(j * m_iRows + i).getButtonClicked();
                if (bIsClicked != true) {
                    bIsBingo = false;
                    break;
                }
            }
            if (bIsBingo) {
                m_iBingoLines++;
                m_binding.tvBingoLines.setText(getResources()
                        .getString(R.string.bingoLines) + m_iBingoLines);
                if (checkGameOver()) {
                    break;
                }
            }
        }

        //左上到右下判斷
        boolean bIsBingo = true;
        for (int i = 0; i < m_iRows; i++) {
            boolean bIsClicked = m_alBingoButton.get(i * m_iRows + i).getButtonClicked();
            if (bIsClicked != true) {
                bIsBingo = false;
                break;
            }
        }
        if (bIsBingo) {
            m_iBingoLines++;
            m_binding.tvBingoLines.setText(getResources()
                    .getString(R.string.bingoLines) + m_iBingoLines);
            checkGameOver();
        }

        //右上到左下判斷
        bIsBingo = true;
        for (int i = 0; i < m_iRows; i++) {
            boolean bIsClicked = m_alBingoButton.get(i * m_iRows + (m_iRows - 1 - i)).getButtonClicked();
            if (bIsClicked != true) {
                bIsBingo = false;
                break;
            }
        }
        if (bIsBingo) {
            m_iBingoLines++;
            m_binding.tvBingoLines.setText(getResources()
                    .getString(R.string.bingoLines) + m_iBingoLines);
            checkGameOver();
        }

        m_iBingoLines = 0;

    }

    public boolean checkGameOver() {
        if (m_iBingoLines >= m_iRows) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("完成Bingo!");
            builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            for (int i = 0; i < m_alBingoButton.size(); i++) {
                EditText etButton = m_alBingoButton.get(i).getEditTextButton();
                etButton.setEnabled(false);
            }
            return true;
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
    public void modeSettings() {
        if (m_iMode == 1) {
            m_iBingoLines=0;
            m_binding.tvBingoLines.setText(getResources()
                    .getString(R.string.bingoLines) + m_iBingoLines);
            m_binding.etNumRange.setEnabled(true);
            m_binding.etRowRange.setEnabled(true);
            m_binding.btnRandom.setClickable(true);
            m_binding.ivRowsCheck.setClickable(true);
            /*for (int i = 0; i < m_binding.rgChangeColor.getChildCount(); i++) {
                m_binding.rgChangeColor.getChildAt(i).setEnabled(true);
            }*/
            for (int i = 0; i < m_alBingoButton.size(); i++) {
                EditText etButton = m_alBingoButton.get(i).getEditTextButton();
                etButton.setLongClickable(true);
                etButton.setCursorVisible(true);
                etButton.setFocusable(true);
                etButton.setFocusableInTouchMode(true);
                etButton.setEnabled(true);
                etButton.setBackgroundColor(m_iColor);
                m_alBingoButton.get(i).setButtonClicked(false);
            }
        } else {
            m_binding.etNumRange.setEnabled(false);
            m_binding.etRowRange.setEnabled(false);
            m_binding.btnRandom.setClickable(false);
            m_binding.ivRowsCheck.setClickable(false);
            m_binding.rgChangeColor.setEnabled(false);
            /*for (int i = 0; i < m_binding.rgChangeColor.getChildCount(); i++) {
                m_binding.rgChangeColor.getChildAt(i).setEnabled(true);
            }*/
            for (int i = 0; i < m_alBingoButton.size(); i++) {
                EditText etButton = m_alBingoButton.get(i).getEditTextButton();
                etButton.setLongClickable(false);
                etButton.setCursorVisible(false);
                etButton.setFocusable(false);
                etButton.setFocusableInTouchMode(false);
            }
        }
    }

    public boolean checkNumRange() {
        String enteredText = m_binding.etNumRange.getText().toString();
        if (!TextUtils.isEmpty(enteredText)) {
            if (Pattern.matches("[1-9]\\d*-[1-9]\\d*",
                    m_binding.etNumRange.getText())) {
                //切割出範圍的最大最小值
                String randomRange = m_binding.etNumRange.getText().toString();
                String[] rangeMinMax = randomRange.split("-");
                int iMin = Integer.parseInt(rangeMinMax[0]);
                int iMax = Integer.parseInt(rangeMinMax[1]);
                if ((iMax - iMin + 1) < m_alBingoButton.size() || iMax > 99) {
                    toastMsg(R.string.inputNumError);
                } else {
                    m_iRangeMin = iMin;
                    m_iRangeMax = iMax;
                    return true;
                }
            } else {
                toastMsg(R.string.inputNumError);
                return false;
            }
        } else {
            toastMsg(R.string.inputEmpty);
            return false;
        }
        return false;
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void toastMsg(int resourceId) {
        String strMsg = getResources().getString(resourceId);
        Toast.makeText(MainActivity.this,
                strMsg,
                Toast.LENGTH_SHORT).show();
    }

}
