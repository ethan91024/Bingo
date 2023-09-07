package com.softmobile.Bingo.ui;

import android.widget.Button;
import android.widget.EditText;

public class BingoButton {
    private EditText m_etButton = null;
    private boolean m_bButtonClicked = false;//true就是被按了
    private int m_iButtonNum = 0;//儲存按鈕的數字

    public BingoButton(EditText etButton, int iTag) {
        m_etButton = etButton;
        m_etButton.setTag(iTag);
    }

    public EditText getEditTextButton() {return m_etButton;}

    public boolean getButtonClicked() {
        return m_bButtonClicked;
    }

    public void setButtonClicked(boolean b) {
        m_bButtonClicked = b;
    }

    public void setButtonNum(int number) {
        m_iButtonNum = number;
    }

    public int getButtonNum() {return m_iButtonNum;}
}
