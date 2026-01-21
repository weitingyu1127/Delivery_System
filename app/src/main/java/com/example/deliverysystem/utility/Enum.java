package com.example.deliverysystem.utility;
import com.example.deliverysystem.BaseActivity;

public class Enum extends BaseActivity {
    public enum place {
        本廠, 倉庫, 線西;
        public static String[] getOptions() {
            return new String[] {
                    "進貨地點",
                    本廠.name(),
                    倉庫.name(),
                    線西.name()
            };
        };
    }
}
