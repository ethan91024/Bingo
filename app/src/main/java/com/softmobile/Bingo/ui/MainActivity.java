package com.softmobile.Bingo.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.softmobile.Bingo.R;
import com.softmobile.Bingo.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding m_binding = null;
    private View m_vBinding = null;
    private final Random m_rRandom = new Random();
    private ArrayList<BingoButton> m_alBingoButton = new ArrayList<>();
    private int m_iBingoLines = 0;
    private int m_iRows = 0;
    private int m_iColor = 0;
    private int m_iMode = 1;//0是遊戲模式，1是輸入模式
    private int m_iRangeMin = 0;
    private int m_iRangeMax = 0;
    //private String m_strPreNum = "";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_binding = ActivityMainBinding.inflate(getLayoutInflater());
        m_vBinding = m_binding.getRoot();
        setContentView(m_vBinding);

        m_iColor = getResources().getColor(R.color.purple_500);
        m_binding.tvBingoLines.setText(getResources()
                .getString(R.string.bingoLines) + " " + m_iBingoLines);
        gameInstruction();

        //判斷當前模式為何
        m_binding.switchMode.setOnCheckedChangeListener(compoundBtnCheckedListener);
        //radioGroup換顏色
        m_binding.rgChangeColor.setOnCheckedChangeListener(rgCheckedChangeListener);
        //偵測數字範圍改變
        m_binding.etNumRange.setOnEditorActionListener(etActionListener);
        //偵測行數改變
        m_binding.etRowRange.setOnEditorActionListener(etActionListener);
        //產生亂數
        m_binding.btnRandom.setOnClickListener(onClickListener);
        //改變賓果的行列數量，加入按鈕及相關設定
        m_binding.ivRowsCheck.setOnClickListener(onClickListener);
    }

    //切換模式的checkedListener
    private final CompoundButton.OnCheckedChangeListener compoundBtnCheckedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (m_binding.switchMode.isChecked()) {
                //變為輸入模式
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.dialogTitle2);
                builder.setMessage(R.string.dialogContent2);
                builder.setPositiveButton(R.string.dialogConfirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        m_iMode = 1;
                        m_iBingoLines = 0;
                        modeSettings();
                    }
                });
                builder.setNegativeButton(R.string.dialogCancel, new DialogInterface.OnClickListener() {
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

    };

    //換顏色的checkedListener
    private final RadioGroup.OnCheckedChangeListener rgCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // 在这里处理RadioButton的变化事件
            if (checkedId == R.id.rbtnRed) {
                m_iColor = getResources().getColor(R.color.red);
            } else if (checkedId == R.id.rbtnOrange) {
                m_iColor = getResources().getColor(R.color.orange);
            } else if (checkedId == R.id.rbtnGreen) {
                m_iColor = getResources().getColor(R.color.green);
            } else if (checkedId == R.id.rbtnPurple) {
                m_iColor = getResources().getColor(R.color.purple_500);
            }
            for (int j = 0; j < m_alBingoButton.size(); j++) {
                m_alBingoButton.get(j).setButtonClicked(false);
                m_alBingoButton.get(j).getEditTextButton().setBackgroundColor(m_iColor);
            }
        }
    };

    //範圍、行數及賓果按鈕的editText Listener
    private final EditText.OnEditorActionListener etActionListener = new TextView.OnEditorActionListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            int iViewId = textView.getId();
            if (iViewId == m_binding.etNumRange.getId()) {
                if (i == EditorInfo.IME_ACTION_DONE ||
                        (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                                keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    checkNumRange();
                    hideKeyboard(MainActivity.this, m_vBinding);
                    return true;
                }
            } else if (iViewId == m_binding.etRowRange.getId()) {
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
                        Log.d("0", "176");
                        toastMsg(R.string.inputEmpty);
                    }
                    hideKeyboard(MainActivity.this, m_vBinding);
                    return true;
                }
            } else {
                //偵測賓果按鈕輸入的數字是否重複或超出範圍
                //按下確認鍵或是(鍵盤事件不為null,鍵盤事件為按下,按下鍵為ENTER)
                if (i == EditorInfo.IME_ACTION_DONE ||
                        (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                                keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    int iNewNum = 0;
                    int iButtonNum = 0;
                    String enteredText = textView.getText().toString();
                    if (!TextUtils.isEmpty(enteredText)) {
                        iNewNum = Integer.parseInt(enteredText);
                        if (iNewNum != 0) {
                            if (iNewNum >= m_iRangeMin && iNewNum <= m_iRangeMax) {
                                for (int j = 0; j < m_alBingoButton.size(); j++) {
                                    iButtonNum = m_alBingoButton.get(j).getButtonNum();
                                    if (iNewNum == iButtonNum) {
                                        //textView.setText(m_strPreNum);
                                        textView.setText("");
                                        toastMsg(R.string.numberExists);
                                        hideKeyboard(MainActivity.this, m_vBinding);
                                        return true;
                                    }
                                }
                                m_alBingoButton.get((Integer) textView.getTag()).setButtonNum(iNewNum);
                                m_alBingoButton.get((Integer) textView.getTag()).getEditTextButton().setText(iNewNum + "");
                            } else {
                                //textView.setText(m_strPreNum);
                                textView.setText("");
                                toastMsg(R.string.outOfRange);
                            }
                        } else {
                            //textView.setText(m_strPreNum);
                            textView.setText("");
                            toastMsg(R.string.zeroNum);
                        }
                    } else {
                        //textView.setText(m_strPreNum);
                        textView.setText("");
                        toastMsg(R.string.inputEmpty);
                    }
                    hideKeyboard(MainActivity.this, m_vBinding);
                    return true;
                }
            }
            return false;
        }
    };

    //亂數按鈕、打勾圖案及賓果按鈕點擊的Listener
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onClick(View view) {
            int iViewId = view.getId();
            if (iViewId == m_binding.btnRandom.getId()) {
                hideKeyboard(MainActivity.this, m_vBinding);
                ArrayList<Integer> alRandomNumbers = new ArrayList<>();
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
            } else if (iViewId == m_binding.ivRowsCheck.getId()) {
                if (!TextUtils.isEmpty(m_binding.etRowRange.getText())) {
                    int enteredNum = Integer.parseInt(m_binding.etRowRange.getText().toString());
                    if (enteredNum != 0) {
                        if (enteredNum >= 2 && enteredNum <= 5) {
                            m_iRows = enteredNum;

                            if (m_binding.bingoTable.getChildCount() != 0) {
                                m_alBingoButton.clear();
                                m_binding.bingoTable.removeAllViews();
                            }
                            createBingo();
                        } else {
                            toastMsg(R.string.inputNumError);
                        }
                    } else {
                        toastMsg(R.string.zeroNum);
                    }
                } else {
                    Log.d("1", "292");
                    toastMsg(R.string.inputEmpty);
                }
                hideKeyboard(MainActivity.this, m_vBinding);
            } else {
                //賓果按鈕點擊事件(換顏色、確認賓果)
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
        }
    };

    //產生賓果盤
    public void createBingo() {
        TableRow row = null;
        EditText etButton = null;
        for (int i = 0; i < m_iRows; i++) {
            row = new TableRow(MainActivity.this);
            row.setGravity(Gravity.CENTER_HORIZONTAL);
            for (int j = 0; j < m_iRows; j++) {
                etButton = new EditText(MainActivity.this);
                BingoButton bingoButton = new BingoButton(etButton, i * m_iRows + j);
                row.addView(etButton);
                m_alBingoButton.add(bingoButton);

                //editText的設定
                etButton.setText("");
                etButton.setId(i * m_iRows + j);
                etButton.setTextSize(25);
                etButton.setInputType(InputType.TYPE_CLASS_NUMBER);
                etButton.setGravity(Gravity.CENTER);
                etButton.setSingleLine();
                etButton.setImeOptions(EditorInfo.IME_ACTION_DONE);
                etButton.setTextColor(Color.WHITE);
                etButton.setBackgroundColor(m_iColor);
                etButton.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
                etButton.getLayoutParams().width = (m_binding.bingoTable.getWidth() / m_iRows);
                etButton.getLayoutParams().height = (m_binding.bingoTable.getHeight() / m_iRows);
                etButton.setOnClickListener(onClickListener);
                etButton.setOnEditorActionListener(etActionListener);
                                    /*etButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                        @Override
                                        public void onFocusChange(View v, boolean hasFocus) {
                                            //在輸入新數字前先記下舊的數字，以便復原
                                            if (!hasFocus) {
                                                m_strPreNum = etButton.getText() + "";
                                            } else {
                                                etButton.setCursorVisible(true);
                                            }
                                        }
                                    });*/
            }
            m_binding.bingoTable.addView(row, i);
        }
    }

    //判斷是否連線
    @SuppressLint("SetTextI18n")
    public void checkBingoLine() {
        m_binding.tvBingoLines.setText(getResources()
                .getString(R.string.bingoLines) + " " + m_iBingoLines);
        boolean bIsBingo = true;
        boolean bIsClicked = false;
        //橫排判斷
        for (int i = 0; i < m_iRows; i++) {
            bIsBingo = true;
            for (int j = 0; j < m_iRows; j++) {
                bIsClicked = m_alBingoButton.get(i * m_iRows + j).getButtonClicked();
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
                        .getString(R.string.bingoLines) + " " + m_iBingoLines);                //如果已Bingo就跳出迴圈
                if (checkGameOver()) {
                    break;
                }
            }
        }

        //直排判斷
        for (int i = 0; i < m_iRows; i++) {
            bIsBingo = true;
            for (int j = 0; j < m_iRows; j++) {
                bIsClicked = m_alBingoButton.get(j * m_iRows + i).getButtonClicked();
                if (!bIsClicked) {
                    bIsBingo = false;
                    break;
                }
            }
            if (bIsBingo) {
                m_iBingoLines++;
                m_binding.tvBingoLines.setText(getResources()
                        .getString(R.string.bingoLines) + " " + m_iBingoLines);
                if (checkGameOver()) {
                    break;
                }
            }
        }

        //左上到右下判斷
        bIsBingo = true;
        for (int i = 0; i < m_iRows; i++) {
            bIsClicked = m_alBingoButton.get(i * m_iRows + i).getButtonClicked();
            if (!bIsClicked) {
                bIsBingo = false;
                break;
            }
        }
        if (bIsBingo) {
            m_iBingoLines++;
            m_binding.tvBingoLines.setText(getResources()
                    .getString(R.string.bingoLines) + " " + m_iBingoLines);
            checkGameOver();
        }

        //右上到左下判斷
        bIsBingo = true;
        for (int i = 0; i < m_iRows; i++) {
            bIsClicked = m_alBingoButton.get(i * m_iRows + (m_iRows - 1 - i)).getButtonClicked();
            if (!bIsClicked) {
                bIsBingo = false;
                break;
            }
        }
        if (bIsBingo) {
            m_iBingoLines++;
            m_binding.tvBingoLines.setText(getResources()
                    .getString(R.string.bingoLines) + " " + m_iBingoLines);
            checkGameOver();
        }

        m_iBingoLines = 0;
    }

    //判斷遊戲結束及跳出AlertDialog
    public boolean checkGameOver() {
        if (m_iBingoLines >= m_iRows) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialogTitle1);
            builder.setPositiveButton(R.string.dialogConfirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            EditText etButton = null;
            for (int i = 0; i < m_alBingoButton.size(); i++) {
                etButton = m_alBingoButton.get(i).getEditTextButton();
                etButton.setEnabled(false);
            }
            return true;
        }
        return false;
    }

    //切換模式後的設定
    @SuppressLint("SetTextI18n")
    public void modeSettings() {
        if (m_iMode == 1) {
            m_iBingoLines = 0;
            m_binding.tvBingoLines.setText(getResources()
                    .getString(R.string.bingoLines) + " " + m_iBingoLines);
            m_binding.etNumRange.setEnabled(true);
            m_binding.etRowRange.setEnabled(true);
            m_binding.btnRandom.setClickable(true);
            m_binding.ivRowsCheck.setClickable(true);
            for (int i = 0; i < m_binding.rgChangeColor.getChildCount(); i++) {
                m_binding.rgChangeColor.getChildAt(i).setEnabled(true);
            }

            EditText etButton = null;
            for (int i = 0; i < m_alBingoButton.size(); i++) {
                etButton = m_alBingoButton.get(i).getEditTextButton();
                etButton.setLongClickable(true);
                etButton.setCursorVisible(true);
                etButton.setFocusable(true);
                etButton.setFocusableInTouchMode(true);
                etButton.setEnabled(true);
                etButton.setBackgroundColor(m_iColor);
                m_alBingoButton.get(i).setButtonClicked(false);
            }
        } else {
            //判斷格子是否都有數字
            String iButtonNum = null;
            for (int i = 0; i < m_alBingoButton.size(); i++) {
                iButtonNum = m_alBingoButton.get(i).getEditTextButton().getText() + "";
                if (TextUtils.isEmpty(iButtonNum) || m_alBingoButton.isEmpty()) {
                    toastMsg(R.string.bingoNotCompleted);
                    m_binding.switchMode.setChecked(true);
                    return;
                }
            }
            m_binding.etNumRange.setEnabled(false);
            m_binding.etRowRange.setEnabled(false);
            m_binding.btnRandom.setClickable(false);
            m_binding.ivRowsCheck.setClickable(false);
            //m_binding.rgChangeColor.setEnabled(false);
            for (int i = 0; i < m_binding.rgChangeColor.getChildCount(); i++) {
                m_binding.rgChangeColor.getChildAt(i).setEnabled(false);
            }
            EditText etButton = null;
            for (int i = 0; i < m_alBingoButton.size(); i++) {
                etButton = m_alBingoButton.get(i).getEditTextButton();
                etButton.setLongClickable(false);
                etButton.setCursorVisible(false);
                etButton.setFocusable(false);
                etButton.setFocusableInTouchMode(false);
            }
        }
    }

    //檢查範圍輸入的格式並存取最小最大值
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

    //隱藏鍵盤
    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    //訊息toast
    public void toastMsg(int resourceId) {
        String strMsg = getResources().getString(resourceId);
        Toast.makeText(MainActivity.this,
                strMsg,
                Toast.LENGTH_SHORT).show();
    }

    public void gameInstruction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialogTitle3);
        builder.setMessage(R.string.dialogContent3);
        builder.setPositiveButton(R.string.dialogUnderstand, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
