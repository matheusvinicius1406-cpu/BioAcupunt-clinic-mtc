import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';

export const isWeb = Platform.OS === 'web' || typeof window !== 'undefined';

export const platformStorage = {
  getItem: async (key: string) => {
    if (isWeb) return localStorage.getItem(key);
    return await AsyncStorage.getItem(key);
  },
  setItem: async (key: string, value: string) => {
    if (isWeb) localStorage.setItem(key, value);
    else await AsyncStorage.setItem(key, value);
  },
  removeItem: async (key: string) => {
    if (isWeb) localStorage.removeItem(key);
    else await AsyncStorage.removeItem(key);
  }
};

export const platformAlert = (message: string, onConfirm?: () => void) => {
  if (isWeb) {
    if (window.confirm(message)) {
      onConfirm?.();
    }
  } else {
    // In React Native, we use Alert.alert (requires 'react-native' import)
    const { Alert } = require('react-native');
    Alert.alert(
      "BioAcupunt",
      message,
      [
        { text: "Cancelar", style: "cancel" },
        { text: "Confirmar", onPress: onConfirm }
      ]
    );
  }
};

export const platformOpen = (url: string) => {
  if (isWeb) {
    window.open(url, '_blank');
  } else {
    const { Linking } = require('react-native');
    Linking.openURL(url);
  }
};
