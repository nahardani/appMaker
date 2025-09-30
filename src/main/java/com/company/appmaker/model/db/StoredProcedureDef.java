package com.company.appmaker.model.db;

import com.company.appmaker.model.FieldDef;

import java.util.ArrayList;
import java.util.List;

/** تعریف Stored Procedure برای پروژه‌های JDBC-محور */
public class StoredProcedureDef {

    public enum ParamMode { IN, OUT, INOUT }

    public static class SpParam {
        private String name;       // نام پارامتر SP
        private String javaType;   // String, Long, ...
        private ParamMode mode = ParamMode.IN;
        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public String getJavaType(){return javaType;}
        public void setJavaType(String javaType){this.javaType=javaType;}
        public ParamMode getMode(){return mode;}
        public void setMode(ParamMode mode){this.mode=mode;}
    }

    public static class CursorDef {
        private String name;                 // نام کرسر
        private List<FieldDef> fields = new ArrayList<>(); // ساختار رکوردهای کرسر
        public String getName(){return name;}
        public void setName(String name){this.name=name;}
        public List<FieldDef> getFields(){return fields;}
        public void setFields(List<FieldDef> fields){this.fields=fields;}
    }

    private String name;                    // نام SP
    private List<SpParam> inputs = new ArrayList<>();
    private List<SpParam> outputs = new ArrayList<>();
    private List<CursorDef> cursors = new ArrayList<>();

    public String getName(){return name;}
    public void setName(String name){this.name=name;}
    public List<SpParam> getInputs(){return inputs;}
    public void setInputs(List<SpParam> inputs){this.inputs=inputs;}
    public List<SpParam> getOutputs(){return outputs;}
    public void setOutputs(List<SpParam> outputs){this.outputs=outputs;}
    public List<CursorDef> getCursors(){return cursors;}
    public void setCursors(List<CursorDef> cursors){this.cursors=cursors;}
}
