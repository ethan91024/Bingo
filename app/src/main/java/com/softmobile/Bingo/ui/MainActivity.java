package com.softmobile.Bingo.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.softmobile.Bingo.R;
import com.softmobile.Bingo.databinding.ActivityMainBinding;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding m_binding = null;
    private View m_vBinding = null;
    private final Random m_rRandom = new Random();
    private ArrayList<BingoButton> m_alBingoButton = new ArrayList<>();
    //private boolean m_bShouldContinue = true;
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
    private final CompoundButton.OnCheckedChangeListener m_compoundBtnCheckedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            //if (m_bShouldContinue) {
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
                builder.create().show();
            } else if (!m_binding.switchMode.isChecked()) {
                //變為遊戲模式
                m_iMode = 0;
                modeSettings();
            }
        }
        //m_bShouldContinue=true;
        //}
    };

    //賓果盤換顏色的checkedListener
    private final RadioGroup.OnCheckedChangeListener m_rgCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
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
                m_alBingoButton.get(j).getEditTextButton().getBackground().setColorFilter(m_iColor, PorterDuff.Mode.SRC_IN);
            }
        }
    };

    //!!!!賓果按鈕判斷時機
    //範圍、行數及賓果按鈕的editText Listener
    private final EditText.OnEditorActionListener m_etActionListener = new TextView.OnEditorActionListener() {
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
                    checkBingoButtonNum(textView);
                }
            }
            return false;
        }
    };

    //亂數按鈕、打勾圖案及賓果按鈕點擊的Listener
    private final View.OnClickListener m_onClickListener = new View.OnClickListener() {
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

                            if (m_binding.bingoRelativeLayout.getChildCount() != 0) {
                                m_alBingoButton.clear();
                                m_binding.bingoRelativeLayout.removeAllViews();
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
                        Drawable backgroundColor = view.getBackground().mutate();
                        backgroundColor.setColorFilter(Color.rgb(Math.max(0, red - 45),
                                Math.max(0, green - 45),
                                Math.max(0, blue - 45)), PorterDuff.Mode.SRC_IN);
                        checkBingoLine();
                    } else {
                        m_alBingoButton.get((Integer) view.getTag()).setButtonClicked(false);
                        view.getBackground().setColorFilter(m_iColor, PorterDuff.Mode.SRC_IN);
                        checkBingoLine();
                    }
                }
            }
        }
    };

    private final View.OnFocusChangeListener m_onFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            int iViewId = view.getId();
            if (iViewId == m_binding.etNumRange.getId()) {
                if (!hasFocus) {
                    checkNumRange();
                    hideKeyboard(MainActivity.this, m_vBinding);
                }
            } else {
                if (!hasFocus) {
                    if (checkBingoButtonNum((TextView) view)) {
                        //m_bShouldContinue = false;
                    }
                }
            }
        }
    };

    //產生賓果盤
    @SuppressLint({"UseCompatLoadingForDrawables", "ClickableViewAccessibility"})
    public void createBingo() {
        EditText etButton = null;
        RelativeLayout relativeLayout = m_binding.bingoRelativeLayout;
        int etButtonWidth = m_binding.bingoRelativeLayout.getWidth() / m_iRows;
        int etButtonHeight = m_binding.bingoRelativeLayout.getHeight() / m_iRows;

        for (int i = 0; i < m_iRows; i++) {
            for (int j = 0; j < m_iRows; j++) {
                etButton = new EditText(MainActivity.this);
                BingoButton bingoButton = new BingoButton(etButton, i * m_iRows + j);

                m_alBingoButton.add(bingoButton);
                RelativeLayout.LayoutParams etParams =
                        new RelativeLayout.LayoutParams(etButtonWidth, etButtonHeight);
                //用設定與左方及上方距離多少的方式來排列按鈕
                etParams.leftMargin = etButtonWidth * j;//j代表橫排第幾個
                etParams.topMargin = etButtonHeight * i;//i代表直排第幾個
                etButton.setText("");
                etButton.setId(i * m_iRows + j);
                etButton.setTextSize(25);
                etButton.setInputType(InputType.TYPE_CLASS_NUMBER);
                etButton.setGravity(Gravity.CENTER);
                etButton.setSingleLine();
                etButton.setFocusableInTouchMode(true);
                etButton.setImeOptions(EditorInfo.IME_ACTION_DONE);
                etButton.setTextColor(Color.WHITE);
                //邊框顯示不出來
                etButton.setBackground(getResources().getDrawable(R.drawable.button_style));
                etButton.getBackground().setColorFilter(m_iColor, PorterDuff.Mode.SRC_IN);

                etButton.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
                etButton.setOnClickListener(m_onClickListener);
                etButton.setOnEditorActionListener(m_etActionListener);
                etButton.setOnFocusChangeListener(m_onFocusChangeListener);
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
                relativeLayout.addView(etButton, etParams);
            }
        }
    }

    public boolean checkBingoButtonNum(TextView textView) {
        int iNewNum = 0;
        int iButtonNum = 0;
        int iButtonIndex = (int) textView.getTag();
        String enteredText = textView.getText().toString();
        if (!TextUtils.isEmpty(enteredText)) {
            iNewNum = Integer.parseInt(enteredText);
            if (iNewNum != 0) {
                if (iNewNum >= m_iRangeMin && iNewNum <= m_iRangeMax) {
                    for (int j = 0; j < m_alBingoButton.size(); j++) {
                        if (j != iButtonIndex) {
                            iButtonNum = m_alBingoButton.get(j).getButtonNum();
                            if (iNewNum == iButtonNum) {
                                //textView.setText(m_strPreNum);
                                textView.setText("");
                                toastMsg(R.string.numberExists);
                                hideKeyboard(MainActivity.this, m_vBinding);
                                return true;
                            }
                        }
                    }
                    m_alBingoButton.get((Integer) textView.getTag()).setButtonNum(iNewNum);
                    m_alBingoButton.get((Integer) textView.getTag()).getEditTextButton().setText(iNewNum + "");
                } else {
                    //textView.setText(m_strPreNum);
                    textView.setText("");
                    toastMsg(R.string.outOfRange);
                    return true;
                }
            } else {
                //textView.setText(m_strPreNum);
                textView.setText("");
                toastMsg(R.string.zeroNum);
                return true;
            }
        } else {
            //textView.setText(m_strPreNum);
            textView.setText("");
            toastMsg(R.string.inputEmpty);
            return true;
        }
        hideKeyboard(MainActivity.this, m_vBinding);
        return false;
    }

    //判斷是否連線
    @SuppressLint("SetTextI18n")
    public void checkBingoLine() {
        m_binding.tvBingoLines.setText(getResources()
                .getString(R.string.bingoLines) + " " + m_iBingoLines);
        //紀錄是否被點擊
        boolean bIsClicked = true;//直線
        boolean bIsClickedDiagnal1 = true;//對角線左上到右下
        boolean bIsClickedDiagnal2 = true;//對角線右上到左下
        //紀錄是否連線
        boolean bIsLine = true;
        boolean bIsLineDiagnal1 = true;
        boolean bIsLineDiagnal2 = true;

        for (int i = 0; i < m_iRows; i++) {
            //橫排
            bIsClicked = true;
            bIsLine = true;
            for (int j = 0; j < m_iRows; j++) {
                bIsClicked = m_alBingoButton.get(i * m_iRows + j).getButtonClicked();
                //有一個沒被點擊就會設為無連線並跳出迴圈
                if (!bIsClicked) {
                    bIsLine = false;
                    break;
                }
            }
            if (bIsLine) {//都按過就會增加連線數
                if (addBingoLines()) {
                    //回傳true就代表遊戲結束並break
                    break;
                }
            }

            //直排
            bIsClicked = true;
            bIsLine = true;
            for (int j = 0; j < m_iRows; j++) {
                bIsClicked = m_alBingoButton.get(j * m_iRows + i).getButtonClicked();
                if (!bIsClicked) {
                    bIsLine = false;
                    break;
                }
            }
            if (bIsLine) {
                if (addBingoLines()) {
                    break;
                }
            }

            //左上到右下
            bIsClickedDiagnal1 = m_alBingoButton.get(i * m_iRows + i).getButtonClicked();
            if (!bIsClickedDiagnal1) {
                bIsLineDiagnal1 = false;
            }

            //右上到左下
            bIsClickedDiagnal2 = m_alBingoButton.get(i * m_iRows + (m_iRows - 1 - i)).getButtonClicked();
            if (!bIsClickedDiagnal2) {
                bIsLineDiagnal2 = false;
            }

        }
        if (bIsLineDiagnal1) {
            addBingoLines();
        }
        if (bIsLineDiagnal2) {
            addBingoLines();
        }

        m_iBingoLines = 0;
    }

    //增加連線數並判斷是否遊戲結束
    public boolean addBingoLines() {
        m_iBingoLines++;
        m_binding.tvBingoLines.setText(getResources()
                .getString(R.string.bingoLines) + " " + m_iBingoLines);
        if (checkGameOver()) {
            return true;
        }
        return false;
    }

    //判斷遊戲結束及跳出AlertDialog
    public boolean checkGameOver() {
        if (m_iBingoLines >= m_iRows) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialogTitle1);
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
                etButton.getBackground().setColorFilter(m_iColor, PorterDuff.Mode.SRC_IN);
                m_alBingoButton.get(i).setButtonClicked(false);
                m_alBingoButton.get(i).setButtonNum(0);
                m_alBingoButton.get(i).getEditTextButton().setText("");
            }
        } else {
            //判斷格子是否都有數字
            String iButtonNum = null;
            if (m_alBingoButton.size() == 0) {
                toastMsg(R.string.bingoNotCreated);
                m_binding.switchMode.setChecked(true);
                m_iMode = 1;
                return;
            }
            for (int i = 0; i < m_alBingoButton.size(); i++) {
                iButtonNum = m_alBingoButton.get(i).getEditTextButton().getText() + "";
                if (TextUtils.isEmpty(iButtonNum)) {
                    toastMsg(R.string.bingoNotCompleted);
                    m_binding.switchMode.setChecked(true);
                    m_iMode = 1;
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
            if (Pattern.matches("\\d{1,2}-\\d{1,2}",
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
