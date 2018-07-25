package com.android.blutoothdemo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ChatAdapter extends BaseAdapter {
    //定义两个类别
    private static final int TYPE_OWN = 0;
    private static final int TYPE_FRIEND = 1;


    private Context mContext;
    private ArrayList<MessageForChat> mData;


    public ChatAdapter(Context mContext,ArrayList<MessageForChat> mData) {
        this.mContext = mContext;
        this.mData = mData;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //多布局的核心，通过这个判断类别
    @Override
    public int getItemViewType(int position) {
        if (mData.get(position).isAuthor()) {
            return TYPE_OWN;
        } else if (!mData.get(position).isAuthor()) {
            return TYPE_FRIEND;
        } else {
            return super.getItemViewType(position);
        }
    }

    //类别数目
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @NonNull
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        int type = getItemViewType(position);
        ViewHolder holder = null;
        if(null == view){
            holder = new ViewHolder();
            switch (type){
                case TYPE_OWN:
                    view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_right, parent, false);
                    holder.textView = view.findViewById(R.id.tv_chat_right);
                    view.setTag(R.id.Tag_OWN,holder);
                    break;
                case TYPE_FRIEND:
                    view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_left, parent, false);
                    holder.textView = view.findViewById(R.id.tv_chat_left);
                    view.setTag(R.id.Tag_FRIEND,holder);
                    break;
            }
        }else{
            switch (type){
                case TYPE_OWN:
                    holder = (ViewHolder) view.getTag(R.id.Tag_OWN);
                    break;
                case TYPE_FRIEND:
                    holder = (ViewHolder) view.getTag(R.id.Tag_FRIEND);
                    break;
            }
        }
        assert holder != null;
        holder.textView.setText(mData.get(position).getContent());
        return view;
    }

    private class ViewHolder{
        TextView textView;
    }
}
