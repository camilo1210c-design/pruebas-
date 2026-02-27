package com.example.parqueadero;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity
{
    private TextView tv;
    private CheckBox auto;
    private CheckBox moto;
    private CheckBox neta;
    private EditText min;
    private EditText placa;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv=(TextView) findViewById(R.id.tv_resultado);
        auto=(CheckBox) findViewById(R.id.ch_auto);
        moto=(CheckBox) findViewById(R.id.ch_moto);
        neta=(CheckBox) findViewById(R.id.ch_camioneta);
        min=(EditText) findViewById(R.id.et_time);
        placa=(EditText) findViewById(R.id.et_pl);
    }

    public void MOSTRAR(View view)
    {
        String pla=placa.getText().toString();
        String tiempo=min.getText().toString();
        String resultado="";
        int time=Integer.parseInt(tiempo);
        double precio_t=0;
        double descuento=0;
        int horas=time/60;
        int minutos =time%60;
        double precion=0;

        if(auto.isChecked()==true)
        {
            if(time>0)
            {
                precio_t=(time*120);
                if(time >= 1 && time<60){
                    descuento=0;
                    precion=precio_t;
                }
                if(time>=60 && time<120){
                    descuento=precio_t*0.1;
                    precio_t=precio_t-descuento;
                    precion=precio_t+descuento;
                }
                if(time>=120 && time<180){
                    descuento=precio_t*0.2;
                    precio_t=precio_t-descuento;
                    precion=precio_t+descuento;
                }
                if(time>=180 ){
                    descuento=precio_t*0.3;
                    precio_t=precio_t-descuento;
                    precion=precio_t+descuento;
                }
            }
            else {
                resultado="tiempo invalido";
                tv.setText(resultado);
            }
            resultado="Placa: "+pla+"\nTiempo: "+horas+" hora(s) y "+minutos+" minuto(s)\nPrecio base: "+precion+"$\nDescuento aplicado: "+descuento+"$\nImporte final: "+precio_t+"$";
            tv.setText(resultado);
        }

        if(moto.isChecked()==true)
        {
            if(time>0)
            {
                precio_t=(time*60);
                if(time >= 1 && time<60){
                    descuento=0;
                    precion=precio_t;
                }
                if(time>=60 && time<120){
                    descuento=precio_t*0.1;
                    precio_t=precio_t-descuento;
                    precion=precio_t+descuento;
                }
                if(time>=120 && time<180){
                    descuento=precio_t*0.2;
                    precio_t=precio_t-descuento;
                    precion=precio_t+descuento;
                }
                if(time>=180 ){
                    descuento=precio_t*0.3;
                    precio_t=precio_t-descuento;
                    precion=precio_t+descuento;
                }
            }
            else
            {
                resultado="tiempo invalido";
                tv.setText(resultado);
            }
            resultado="Placa: "+pla+"\nTiempo: "+horas+" hora(s) y "+minutos+" minuto(s)\nPrecio base: "+precion+"$\nDescuento aplicado: "+descuento+"$\nImporte final: "+precio_t+"$";
            tv.setText(resultado);
        }

        if(neta.isChecked()==true)
        {
            if(time>0)
            {
                precio_t=(time*300);
                if(time >= 1 && time<60){
                    descuento=0;
                    precion=precio_t;
                }
                if(time>=60 && time<120){
                    descuento=precio_t*0.1;
                    precio_t=precio_t-descuento;
                    precion=precio_t+descuento;
                }
                if(time>=120 && time<180){
                    descuento=precio_t*0.2;
                    precio_t=precio_t-descuento;
                    precion=precio_t+descuento;
                }
                if(time>=180 ){
                    descuento=precio_t*0.3;
                    precio_t=precio_t-descuento;
                    precion=precio_t+descuento;
                }
            }
            else {
                resultado="tiempo invalido";
                tv.setText(resultado);
            }
            resultado="Placa: "+pla+"\nTiempo: "+horas+" hora(s) y "+minutos+" minuto(s)\nPrecio base: "+precion+"$\nDescuento aplicado: "+descuento+"$\nImporte final: "+precio_t+"$";
            tv.setText(resultado);
        }
    }
}