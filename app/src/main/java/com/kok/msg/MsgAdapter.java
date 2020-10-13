package com.kok.msg;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {

    private List<SmsEntity> smsList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        View msgView;
        TextView idTv, conentTv, numberTv, dateTv, tv_new;
        Button msgBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            msgView = itemView;
            idTv = (TextView) itemView.findViewById(R.id.tv_id);
            conentTv = (TextView) itemView.findViewById(R.id.tv_content);
            numberTv = (TextView) itemView.findViewById(R.id.tv_number);
            dateTv = (TextView) itemView.findViewById(R.id.tv_date);
            msgBtn = (Button) itemView.findViewById(R.id.btn_msg);
            tv_new = (TextView) itemView.findViewById(R.id.tv_new);
        }

    }

    public MsgAdapter(List<SmsEntity> crushList) {
        smsList = crushList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    public interface OnMsgClick {
        void msgBtnClick(ViewHolder holder, int position);
    }

    private OnMsgClick onMsgClick;

    public void setOnMsgClick(OnMsgClick onMsgClick) {
        this.onMsgClick = onMsgClick;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        SmsEntity smsEntity = smsList.get(position);
        holder.idTv.setText("" + smsEntity.get_id());
        holder.numberTv.setText("" + smsEntity.getAddress());
        holder.conentTv.setText("" + smsEntity.getBody());
        holder.dateTv.setText("" + smsEntity.getDate());
        int type = smsEntity.getState();
        if (position < 5) {
            holder.tv_new.setVisibility(View.VISIBLE);
        } else {
            holder.tv_new.setVisibility(View.INVISIBLE);
        }
        holder.msgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onMsgClick != null) {
                    onMsgClick.msgBtnClick(holder, position);
                }
            }
        });
        if (type == 1) {
            holder.msgBtn.setClickable(false);
            holder.msgBtn.setText("完成");
        } else {
            //判断是否过期
            if (smsEntity.isOutDate) {
                holder.msgBtn.setClickable(false);
                if(smsEntity.isUploadError){
                    holder.msgBtn.setText("失败");
                }else{
                    holder.msgBtn.setText("已超时");
                }

            } else {
                holder.msgBtn.setClickable(true);
                holder.msgBtn.setText("回调");
            }
        }
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }


}
