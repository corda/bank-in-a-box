import common_de from './translations/de/common.json';
import common_en from './translations/en/common.json';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

const lang = localStorage.getItem('lang');

i18n.use(initReactI18next).init({
    resources: {
        en: { common: common_en },
        //Currently de only has a translation for the welcome message on home page as a sample to show how it works
        //Ideally in the future if a translator is hired/translations obtain the en file will be coppied and translated
        //The language needs to be saved in localstorage
        de: { common: common_de },
    },
    lng: lang ? lang : 'en',

    //Will be used if translations for the current language are not available
    fallbackLng: 'en',

    interpolation: {
        //React already handles escaping to avoid xss injection
        escapeValue: false,
    },
});

export default i18n;
