﻿using Microsoft.Win32;
using System.Linq;
using System.Windows;
using System.Collections.Generic;
using System;
using System.Text.RegularExpressions;
using System.IO;
using System.Text;
using System.Windows.Forms;

namespace Hermex.Classes
{
    public class Utils
    {
        public static bool IsWindowOpen<T>(string name = "") where T : System.Windows.Window
        {
            return string.IsNullOrEmpty(name)
               ? App.Current.Windows.OfType<T>().Any()
               : App.Current.Windows.OfType<T>().Any(w => w.Name.Equals(name));
        }

        public static void CheckRunAtStartup()
        {
            var registryKey = AppConstants.StartUpRegistryKey;

            if (AppSettings.Get<bool>("RunAtStartup"))
            {
                using (RegistryKey key = Registry.CurrentUser.OpenSubKey(registryKey, true))
                {
                    key.SetValue(AppConstants.StartUpRegistryKeyName, "\"" + System.Reflection.Assembly.GetExecutingAssembly().Location + "\"");
                }
            }
            else
            {
                using (RegistryKey key = Registry.CurrentUser.OpenSubKey(registryKey, true))
                {
                    key.DeleteValue(AppConstants.StartUpRegistryKeyName, false);
                }
            }
        }

        public static string CoolMs(string ms)
        {
            if (ms.Length == 1)
            {
                return "00" + ms;
            }
            else if (ms.Length == 2)
            {
                return "0" + ms;
            }
            else
            {
                return ms;
            }
        }

        public static string GenerateName()
        {
            string filename = DateTime.Now.ToString("ddMMyy-HHmmss");
            return new Random().Next(999) + "-" + filename;
        }

        public static string GenerateName(string name)
        {
            string st = name;
            st = Regex.Replace(st, "[-+^,èòàù()%&:\\[\\]{\\}]", "");
            st = st.Replace(" ", "_");
            return new Random().Next(99999) + "-" + st;
        }

        public static string SaveImageToLocalPath(string filename)
        {
            return AppSettings.Get<string>("SaveLocalPath") + Path.DirectorySeparatorChar + AppConstants.SaveLocalFileNamePrefix + filename;
        }

        public static string SaveFileToTempPath(string filename)
        {
            return AppConstants.TempFolder + AppConstants.SaveTempFileNamePrefix + filename;
        }

        public static string GetStringCombination(HashSet<int> keyValues)
        {
            StringBuilder output = new StringBuilder();
            foreach(var item in keyValues)
            {
                output.Append(AppConstants.SupportedKeys[(Keys)item]);
                output.Append("+");
            }
            output.Remove(output.ToString().LastIndexOf("+"), 1);
            return output.ToString();
        }
    }
}