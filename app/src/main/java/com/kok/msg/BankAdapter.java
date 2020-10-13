package com.kok.msg;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BankAdapter extends RecyclerView.Adapter<BankAdapter.ViewHolder> {

    private List<BankCardEntity> bankCardList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        View msgView;
        TextView bankNameTv,bankNumberTv;
        Button deleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            msgView = itemView;
            bankNameTv = (TextView) itemView.findViewById(R.id.tv_bank_name);
            bankNumberTv = (TextView) itemView.findViewById(R.id.tv_bank_number);
            deleteBtn   = (Button) itemView.findViewById(R.id.btn_delete);
        }

    }

    public BankAdapter(List<BankCardEntity> crushList) {
        bankCardList = crushList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_msg, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        BankCardEntity bankEntity = bankCardList.get(position);
//        holder.bankNameTv.setText(bankEntity.getBankName());
//        holder.bankNumberTv.setText(bankEntity.getBankNumber());
//        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                bankCardList.remove(position);
//                notifyItemRemoved(position);
////              notifyDataSetChanged();
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return bankCardList.size();
    }


}
