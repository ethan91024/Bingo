package com.softmobile.Bingo.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.softmobile.Bingo.R;
import com.softmobile.Bingo.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private Handler handler = new Handler();

    private int m_iColor = R.drawable.button_purple;
    private static final int PURPLE_STYLE = R.drawable.button_purple;
    private static final int GREEN_STYLE = R.drawable.button_green;
    private static final int ORANGE_STYLE = R.drawable.button_orange;
    private static final int RED_STYLE = R.drawable.button_red;
    private ActivityMainBinding m_binding = null;
    private View m_vBinding = null;
    private final Random m_rRandom = new Random();
    private ArrayList<BingoButton> m_alBingoButton = new ArrayList<>();
    private RecyclerView m_recyclerView;
    private BingoAdapter m_bingoAdapter;
    private int m_iRows = 3;
    private int m_iMode = INPUT_MODE;//0是遊戲模式，1是輸入模式
    private static final int GAME_MODE = 0;
    private static final int INPUT_MODE = 1;
    private int m_iRangeMin = 0;
    private int m_iRangeMax = 0;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_binding = ActivityMainBinding.inflate(getLayoutInflater());
        m_vBinding = m_binding.getRoot();
        setContentView(m_vBinding);

        m_binding.tvBingoLines.setText(getResources()
                .getString(R.string.bingoLines) + "0");
        gameInstruction();

        createBingo();
        //判斷當前模式為何
        m_binding.switchMode.setOnCheckedChangeListener(m_compoundBtnCheckedListener);
        //radioGroup換顏色
        m_binding.rgChangeColor.setOnCheckedChangeListener(m_rgCheckedChangeListener);
        //偵測數字範圍改變(Keyboard)
        m_binding.etNumRange.setOnEditorActionListener(m_etActionListener);
        //偵測行數改變
        m_binding.etRowRange.setOnEditorActionListener(m_etActionListener);
        //產生亂數
        m_binding.btnRandom.setOnClickListener(m_onClickListener);
        //改變賓果的行列數量，加入按鈕及相關設定
        m_binding.ivRowsCheck.setOnClickListener(m_onClickListener);
        //偵測數字範圍改變(Focus)
        m_binding.etNumRange.setOnFocusChangeListener(m_onFocusChangeListener);
    }

    //切換模式的checkedListener
    public CompoundButton.OnCheckedChangeListener m_compoundBtnCheckedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            LinearLayout llRootView = m_binding.llRootView;
            llRootView.clearFocus();
            if (m_binding.switchMode.isChecked()) {
                //變為輸入模式
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.dialogTitle2);
                builder.setMessage(R.string.dialogContent2);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.dialogConfirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        modeSettings(INPUT_MODE);
                        m_bingoAdapter.updateMode(m_iMode);
                    }
                });
                builder.setNegativeButton(R.string.dialogCancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        m_binding.switchMode.setChecked(false);
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            } else if (!m_binding.switchMode.isChecked()) {
                //變為遊戲模式
                if (modeSettings(GAME_MODE)) {
                    m_bingoAdapter.updateMode(m_iMode);
                }
            }
        }
    };

    //賓果盤換顏色的checkedListener
    public RadioGroup.OnCheckedChangeListener m_rgCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // 在这里处理RadioButton的变化事件
            LinearLayout llRootView = m_binding.llRootView;
            llRootView.clearFocus();
            EditText etButton = null;
            if (checkedId == R.id.rbtnPurple) {
                m_iColor = PURPLE_STYLE;
            } else if (checkedId == R.id.rbtnRed) {
                m_iColor = RED_STYLE;
            } else if (checkedId == R.id.rbtnOrange) {
                m_iColor = ORANGE_STYLE;
            } else if (checkedId == R.id.rbtnGreen) {
                m_iColor = GREEN_STYLE;
            }
            for (int j = 0; j < m_alBingoButton.size(); j++) {
                etButton = m_alBingoButton.get(j).getEditTextButton();
                m_alBingoButton.get(j).setButtonClicked(false);
                etButton.setBackgroundResource(m_iColor);
            }
            m_bingoAdapter.updateBingoButton(m_alBingoButton);
        }
    };

    //範圍、行數按鈕的editText ActionListener
    public EditText.OnEditorActionListener m_etActionListener = new TextView.OnEditorActionListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            int iViewId = textView.getId();
            if (iViewId == m_binding.etNumRange.getId()) {
                if (i == EditorInfo.IME_ACTION_DONE ||
                        (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                                keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (!checkNumRange()) {
                        textView.setText("");
                    }
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
                                m_bingoAdapter.updateRows(m_iRows);
                            } else {
                                toastMsg(R.string.inputNumError);
                            }
                        } else {
                            toastMsg(R.string.zeroNum);
                        }
                    } else {
                        toastMsg(R.string.inputEmpty);
                    }
                    hideKeyboard(MainActivity.this, m_vBinding);
                    return true;
                }
            }
            return false;
        }
    };

    //亂數按鈕、打勾圖案ClickListener
    public View.OnClickListener m_onClickListener = new View.OnClickListener() {
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
                        m_bingoAdapter.updateBingoButton(m_alBingoButton);
                    }
                } else {
                    m_binding.etNumRange.setText("");
                }
            } else if (iViewId == m_binding.ivRowsCheck.getId()) {
                if (!TextUtils.isEmpty(m_binding.etRowRange.getText())) {
                    int enteredNum = Integer.parseInt(m_binding.etRowRange.getText().toString());
                    if (enteredNum != 0) {
                        if (enteredNum >= 3 && enteredNum <= 5) {
                            m_iRows = enteredNum;
                            if (m_binding.recyclerView.getChildCount() != 0) {
                                m_alBingoButton.clear();
                                m_binding.recyclerView.removeAllViews();
                                m_bingoAdapter.updateRows(m_iRows);
                                m_bingoAdapter.updateBingoButton(m_alBingoButton);
                            }
                            createBingo();
                        } else {
                            toastMsg(R.string.inputNumError);
                        }
                    } else {
                        toastMsg(R.string.zeroNum);
                    }
                } else {
                    toastMsg(R.string.inputEmpty);
                }
                hideKeyboard(MainActivity.this, m_vBinding);
            }
        }
    };

    //數字範圍editText的focus消失時判斷格式是否正確
    public View.OnFocusChangeListener m_onFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            int iViewId = view.getId();
            if (iViewId == m_binding.etNumRange.getId()) {
                if (!hasFocus) {
                    if (!checkNumRange()) {
                        m_binding.etNumRange.setText("");
                    }
                    hideKeyboard(MainActivity.this, m_vBinding);
                }
            }
        }
    };

    //透過Adapter產生賓果盤
    @SuppressLint({"UseCompatLoadingForDrawables", "ClickableViewAccessibility"})
    public void createBingo() {
        m_recyclerView = findViewById(R.id.recyclerView);
        m_recyclerView.setLayoutManager(new GridLayoutManager(this, m_iRows) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        m_bingoAdapter = new BingoAdapter(m_iRows, m_iColor, m_recyclerView,
                m_alBingoButton, this, m_binding, m_vBinding);
        m_recyclerView.setAdapter(m_bingoAdapter);
    }

    //切換模式後的設定
    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
    public boolean modeSettings(int mode) {
        if (mode == INPUT_MODE) {
            m_binding.tvBingoLines.setText(getResources()
                    .getString(R.string.bingoLines) + "0");
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
                etButton.setActivated(false);
                m_alBingoButton.get(i).setButtonClicked(false);
                m_alBingoButton.get(i).setButtonNum(0);
                m_alBingoButton.get(i).getEditTextButton().setText("");
            }
            m_iMode = INPUT_MODE;
        } else {
            //判斷格子是否都有數字
            int iButtonNum = 0;
            if (m_alBingoButton.size() == 0) {
                toastMsg(R.string.bingoNotCreated);
                m_binding.switchMode.setChecked(true);
                return false;
            }
            for (int i = 0; i < m_alBingoButton.size(); i++) {
                iButtonNum = m_alBingoButton.get(i).getButtonNum();
                if (iButtonNum == 0) {
                    toastMsg(R.string.bingoNotCompleted);
                    m_binding.switchMode.setChecked(true);
                    return false;
                }
                if (TextUtils.isEmpty(m_binding.etNumRange.getText())) {
                    toastMsg(R.string.inputNumError);
                    m_binding.switchMode.setChecked(true);
                    return false;
                }
                if (iButtonNum < m_iRangeMin || iButtonNum > m_iRangeMax) {
                    toastMsg(R.string.outOfRange);
                    m_binding.switchMode.setChecked(true);
                    return false;
                }
            }
            m_binding.etNumRange.setEnabled(false);
            m_binding.etRowRange.setEnabled(false);
            m_binding.btnRandom.setClickable(false);
            m_binding.ivRowsCheck.setClickable(false);
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
            hideKeyboard(this, m_vBinding);
            m_iMode = GAME_MODE;
        }
        return true;
    }

    //檢查範圍輸入的格式並存取最小最大值
    public boolean checkNumRange() {
        String enteredText = m_binding.etNumRange.getText().toString();
        if (!TextUtils.isEmpty(enteredText)) {
            if (Pattern.matches("\\d{1,2}-\\d{1,2}",
                    m_binding.etNumRange.getText())) {
                //切割出範圍的最大最小值
                String randomRange = m_binding.etNumRange.getText().toString();
                String[] rangeMinMax = randomRange.split("-");
                int iMin = Integer.parseInt(rangeMinMax[0]);
                int iMax = Integer.parseInt(rangeMinMax[1]);
                if ((iMax - iMin + 1) < m_alBingoButton.size() || iMax > 99) {
                    toastMsg(R.string.inputNumError);
                    return false;
                } else {
                    m_iRangeMin = iMin;
                    m_iRangeMax = iMax;
                    m_bingoAdapter.updateRange(m_iRangeMin, m_iRangeMax);
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

    //開啟app時的AlertDialog
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
        builder.create().show();
    }
}
