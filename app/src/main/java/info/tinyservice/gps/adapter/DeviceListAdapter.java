
/*
 * Copyright 2016 Irving Gonzalez (ialexis93@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.tinyservice.gps.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import info.tinyservice.gps.R;
import info.tinyservice.gps.model.Device;

public class DeviceListAdapter extends ArrayAdapter<Device> {

    private final String TAG = "DeviceListAdapter";
    private final List<Device> mItems;
    private final Context mContext;
    private final LayoutInflater mInflater;

    private final SimpleDateFormat dateFormatOutput = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
    private final SimpleDateFormat dateFormatInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");



    public DeviceListAdapter (Context context, int resourceId, List<Device> items) {
        super(context, resourceId, items);
        mContext = context;
        mItems = items;
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.device_item_list, null);
            holder = new ViewHolder();
            holder.name = (TextView)convertView.findViewById(R.id.deviceName);
            holder.placa = (TextView)convertView.findViewById(R.id.devicePlaca);
            holder.lastUpdate = (TextView)convertView.findViewById(R.id.lastupdateDevice);
            holder.company = (TextView)convertView.findViewById(R.id.deviceCompany);
            holder.image = (ImageView)convertView.findViewById(R.id.deviceImage);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        Device item = getItem(position);
        if (item != null) {
            // This is where you set up the views.
            // This is just an example of what you could do.
            holder.placa.setText("("+item.getPlaca()+")");
            holder.name.setText(item.getName());
            holder.company.setText(item.getCompany());
            holder.image.setImageBitmap(getImage(item.getImage()));

            if(item.getLastupdate() != null){
                try {
                    Date result =  dateFormatInput.parse(item.getLastupdate().replaceAll("Z$", "+0000"));
                    holder.lastUpdate.setText(dateFormatOutput.format(result));
                } catch (ParseException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

        }
        return convertView;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Device getItem(int position) {
        return mItems.get(position);
    }

    public class ViewHolder {
        TextView placa;
        TextView name;
        TextView lastUpdate;
        TextView company;
        ImageView image;
    }


    private Bitmap getImage(String image) {
        switch (image) {
            case "001.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v001);
            case "002.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v002);
            case "003.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v003);
            case "004.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v004);
            case "005.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v005);
            case "006.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v006);
            case "007.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v007);
            case "008.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v008);
            case "009.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v009);
            case "010.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v010);
            case "011.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v011);
            case "012.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v012);
            case "013.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v013);
            case "016.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v016);
            case "017.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v017);
            case "021.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v021);
            case "023.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v023);
            case "024.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v024);
            case "025.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v025);
            case "026.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v026);
            case "027.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v027);
            case "041.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v041);
            case "042.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v042);
            case "043.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v043);
            case "044.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v044);
            case "045.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v045);
            case "046.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v046);
            case "047.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v047);
            case "048.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v048);
            case "049.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v049);
            case "050.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v050);
            case "053.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v053);
            case "054.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v054);
            case "055.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v055);
            case "058.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v058);
            case "061.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v061);
            case "062.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v062);
            case "070.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.v070);
            case "Circulo.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.vcirculo);
            case "dump_truck.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.vdump_truck);
            case "tanker.png":
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.vtanker);
            default:
                return BitmapFactory.decodeResource(getContext().getResources(), R.drawable.vcirculo);
        }
    }

}
