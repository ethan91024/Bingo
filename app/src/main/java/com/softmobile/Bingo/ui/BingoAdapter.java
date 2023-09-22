package com.softmobile.Bingo.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.softmobile.Bingo.R;
import com.softmobile.Bingo.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class BingoAdapter extends RecyclerView.Adapter<BingoAdapter.BingoViewHolder> {

    private int m_iColor = 0;
    private int m_iBingoLines = 0;
    private int m_iRows = 0;
    private int m_iMode = INPUT_MODE;
    private static final int GAME_MODE = 0;
    private static final int INPUT_MODE = 1;
    private int m_iRangeMin = 0;
    private int m_iRangeMax = 0;
    private int m_etButtonWidth;
    private int m_etButtonHeight;
    private ActivityMainBinding m_binding;
    private View m_vBinding;
    private Activity mainActivity;
    private ArrayList<BingoButton> m_alBingoButton = new ArrayList<>();
    private RecyclerView.LayoutParams m_layoutParams;

    //建構
    public BingoAdapter(int rows, int m_iColor, RecyclerView recyclerView, ArrayList<BingoButton> m_alBingoButton,
                        Activity mainActivity, ActivityMainBinding m_binding, View m_vBinding) {
        this.m_iRows = rows;
        this.m_iColor = m_iColor;
        this.mainActivity = mainActivity;
        this.m_vBinding = m_vBinding;
        this.m_etButtonWidth = recyclerView.getWidth() / m_iRows;
        this.m_etButtonHeight = recyclerView.getHeight() / m_iRows;
        this.m_alBingoButton = m_alBingoButton;
        this.m_binding = m_binding;
        m_layoutParams = new RecyclerView.LayoutParams(
                m_etButtonWidth, m_etButtonHeight);
    }

    @NonNull
    @Override
    public BingoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //建立ViewHolder，並動態調整賓果按鈕的長寬
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.bingo_cell_text, parent, false);
        view.setLayoutParams(m_layoutParams);
        return new BingoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BingoViewHolder holder, int position) {
        //根據位置將etButton跟ViewHolder綁定在一起
        holder.etButton.setText("");
        BingoButton bingoButton = new BingoButton(holder.etButton, position);
        m_alBingoButton.add(bingoButton);
        holder.etButton.setOnClickListener(m_onClickListener);
        holder.etButton.setOnEditorActionListener(m_onEditorActionListener);

        holder.etButton.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (null != m_runnable) {
                    m_handler.removeCallbacks(m_runnable);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                m_runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!TextUtils.isEmpty(editable)) {
                            if (checkBingoButtonNum(holder.etButton, Integer.parseInt(String.valueOf(editable)))) {
                                m_alBingoButton.get((Integer) holder.etButton.getTag()).setButtonNum(Integer.parseInt(editable + ""));
                            } else {
                                holder.etButton.setText("");
                                m_alBingoButton.get((Integer) holder.etButton.getTag()).setButtonNum(0);
                                hideKeyboard(mainActivity, m_vBinding);
                            }
                        } else {
                            m_alBingoButton.get((Integer) holder.etButton.getTag()).setButtonNum(0);
                        }
                    }
                };
                m_handler.postDelayed(m_runnable, 1500);
            }
        });
    }

    private Handler m_handler = new Handler(Looper.getMainLooper());
    private Runnable m_runnable = null;

    //讓賓果輸入數字時可以按Enter鍵退下鍵盤
    private TextView.OnEditorActionListener m_onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (i == EditorInfo.IME_ACTION_DONE ||
                    (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                            keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                hideKeyboard(mainActivity, m_vBinding);
            }
            return false;
        }
    };

    //賓果按鈕的點擊事件，會透過切換Activated狀態在selector中變顏色
    private View.OnClickListener m_onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (m_iMode == GAME_MODE) {
                if (view.isActivated()) {
                    view.setActivated(false);
                } else {
                    view.setActivated(true);
                }
                if (!m_alBingoButton.get((Integer) view.getTag()).getButtonClicked()) {
                    m_alBingoButton.get((Integer) view.getTag()).setButtonClicked(true);
                    checkBingoLine();
                } else {
                    m_alBingoButton.get((Integer) view.getTag()).setButtonClicked(false);
                    checkBingoLine();
                }
            }
        }
    };

    @Override
    public int getItemCount() {
        return m_iRows * m_iRows;
    }

    public class BingoViewHolder extends RecyclerView.ViewHolder {
        EditText etButton;

        //設定賓果按鈕的屬性及Drawable
        public BingoViewHolder(@NonNull View itemView) {
            super(itemView);
            etButton = itemView.findViewById(R.id.etButton);
            etButton.setBackgroundResource(m_iColor);
        }
    }

    //檢查賓果的數字是否為空、為0、超出範圍、重複
    @SuppressLint("SetTextI18n")
    public boolean checkBingoButtonNum(TextView textView, int iNewNum) {
        int iButtonNum = 0;
        int iButtonIndex = (int) textView.getTag();
        if (!TextUtils.isEmpty(iNewNum + "")) {
            if (iNewNum != 0) {
                if (iNewNum >= m_iRangeMin && iNewNum <= m_iRangeMax) {
                    for (int j = 0; j < m_alBingoButton.size(); j++) {
                        if (j != iButtonIndex) {
                            iButtonNum = m_alBingoButton.get(j).getButtonNum();
                            if (iNewNum == iButtonNum) {
                                toastMsg(R.string.numberExists);
                                return false;
                            }
                        }
                    }
                    return true;
                } else {
                    toastMsg(R.string.outOfRange);
                    return false;
                }
            } else {
                toastMsg(R.string.zeroNum);
                return false;
            }
        } else {
            toastMsg(R.string.inputEmpty);
            return false;
        }
    }

    //判斷是否連線
    @SuppressLint("SetTextI18n")
    public void checkBingoLine() {
        m_binding.tvBingoLines.setText(mainActivity.getResources()
                .getString(R.string.bingoLines) + m_iBingoLines);
        //紀錄是否被點擊
        boolean bIsClickedHorizontal = true;//橫排
        boolean bIsClickedVertical = true;//直排
        boolean bIsClickedDiagnal1 = true;//對角線左上到右下
        boolean bIsClickedDiagnal2 = true;//對角線右上到左下
        //紀錄是否連線
        boolean bIsLineHorizontal = true;
        boolean bIsLineVertical = true;
        boolean bIsLineDiagnal1 = true;
        boolean bIsLineDiagnal2 = true;

        for (int i = 0; i < m_iRows; i++) {
            //橫排
            bIsClickedHorizontal = true;
            bIsClickedVertical = true;
            bIsLineHorizontal = true;
            bIsLineVertical = true;
            for (int j = 0; j < m_iRows; j++) {
                bIsClickedHorizontal = m_alBingoButton.get(i * m_iRows + j).getButtonClicked();
                bIsClickedVertical = m_alBingoButton.get(j * m_iRows + i).getButtonClicked();
                //有一個沒被點擊就會設為無連線
                if (!bIsClickedHorizontal) {
                    bIsLineHorizontal = false;
                }
                if (!bIsClickedVertical) {
                    bIsLineVertical = false;
                }
            }
            if (bIsLineHorizontal) {
                //都按過就會增加連線數
                if (addBingoLines()) {
                    //回傳true就代表遊戲結束並break
                    break;
                }
            }
            if (bIsLineVertical) {
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
    @SuppressLint("SetTextI18n")
    public boolean addBingoLines() {
        m_iBingoLines++;
        m_binding.tvBingoLines.setText(mainActivity.getResources()
                .getString(R.string.bingoLines) + m_iBingoLines);
        if (checkGameOver()) {
            return true;
        }
        return false;
    }

    //判斷遊戲結束及跳出AlertDialog
    public boolean checkGameOver() {
        if (m_iBingoLines >= m_iRows) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setTitle(R.string.dialogTitle1);
            builder.setCancelable(false);//設定不能點擊對話框以外的地方
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

    //隱藏鍵盤
    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    //訊息toast
    public void toastMsg(int resourceId) {
        String strMsg = mainActivity.getResources().getString(resourceId);
        Toast.makeText(mainActivity,
                strMsg,
                Toast.LENGTH_SHORT).show();
    }

    //將MainActivity的變數同步更新到Adapter中
    public void updateMode(int m_iMode) {
        this.m_iMode = m_iMode;
    }

    public void updateRange(int m_iRangeMin, int m_iRangeMax) {
        this.m_iRangeMin = m_iRangeMin;
        this.m_iRangeMax = m_iRangeMax;
    }

    public void updateRows(int m_iRows) {
        this.m_iRows = m_iRows;
    }

    public void updateBingoButton(ArrayList<BingoButton> m_alBingoButton) {
        this.m_alBingoButton = m_alBingoButton;
    }
}
