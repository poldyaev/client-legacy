﻿using Hermex.Classes;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Windows;
using Hermex.Windows;

namespace Hermex
{
    public partial class App : Application
    {
        private SystemTray Tray;
        public KeyListener GlobalKeyListener;

        protected override void OnStartup(StartupEventArgs e)
        {
            Logger.Set(AppConstants.Name + " started!");

            //no double instance!
            if(Process.GetProcessesByName(Process.GetCurrentProcess().ProcessName).Length > 1)
            {
                MessageBox.Show(AppConstants.Name + " is already running.", "Error");
                Current.Shutdown();
                return;
            }
           
            Current.ShutdownMode = ShutdownMode.OnExplicitShutdown;

            AppSettings.InitializeSettings();
            Utils.CheckRunAtStartup();
            
            GlobalKeyListener = new KeyListener();
            GlobalKeyListener.EnableListener();
            Tray = new SystemTray();
        }
        
    }
}