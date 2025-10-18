package com.example.hifiwifi.ui.adapters;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hifiwifi.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for BLE device list
 */
public class BLEDeviceAdapter extends RecyclerView.Adapter<BLEDeviceAdapter.DeviceViewHolder> {
    
    private List<BluetoothDevice> devices;
    private OnDeviceClickListener clickListener;
    
    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }
    
    public BLEDeviceAdapter(List<BluetoothDevice> devices, OnDeviceClickListener clickListener) {
        this.devices = devices != null ? new ArrayList<>(devices) : new ArrayList<>();
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ble_device, parent, false);
        return new DeviceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        holder.bind(device, clickListener);
    }
    
    @Override
    public int getItemCount() {
        return devices.size();
    }
    
    public void updateDevices(List<BluetoothDevice> newDevices) {
        this.devices = newDevices != null ? new ArrayList<>(newDevices) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView textDeviceName;
        private TextView textDeviceAddress;
        private Button buttonConnect;
        
        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            textDeviceName = itemView.findViewById(R.id.text_device_name);
            textDeviceAddress = itemView.findViewById(R.id.text_device_address);
            buttonConnect = itemView.findViewById(R.id.button_connect);
        }
        
        public void bind(BluetoothDevice device, OnDeviceClickListener clickListener) {
            String deviceName = device.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unknown Device";
            }
            
            textDeviceName.setText(deviceName);
            textDeviceAddress.setText(device.getAddress());
            
            // Set click listener
            buttonConnect.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onDeviceClick(device);
                }
            });
        }
    }
}
