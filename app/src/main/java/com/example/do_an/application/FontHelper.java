package com.example.do_an.application;

import android.content.Context;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;
import com.example.do_an.R;

/**
 * FontHelper - Singleton class để quản lý fonts
 * Sử dụng: FontHelper.getInstance(context).getBold()
 */
public class FontHelper {

    private static FontHelper instance;

    // Cached typefaces để tránh load lại nhiều lần
    // Poppins - Main app font
    private Typeface poppinsLight;
    private Typeface poppinsRegular;
    private Typeface poppinsMedium;
    private Typeface poppinsSemiBold;
    private Typeface poppinsBold;

    // League Spartan - Auth screens font
    private Typeface leagueSpartanLight;
    private Typeface leagueSpartanRegular;
    private Typeface leagueSpartanMedium;
    private Typeface leagueSpartanSemiBold;
    private Typeface leagueSpartanBold;

    private FontHelper(Context context) {
        // Load tất cả fonts một lần duy nhất
        // Poppins
        poppinsLight = ResourcesCompat.getFont(context, R.font.poppins_light);
        poppinsRegular = ResourcesCompat.getFont(context, R.font.poppins_regular);
        poppinsMedium = ResourcesCompat.getFont(context, R.font.poppins_medium);
        poppinsSemiBold = ResourcesCompat.getFont(context, R.font.poppins_semibold);
        poppinsBold = ResourcesCompat.getFont(context, R.font.poppins_bold);

        // League Spartan
        leagueSpartanLight = ResourcesCompat.getFont(context, R.font.league_spartan_light);
        leagueSpartanRegular = ResourcesCompat.getFont(context, R.font.league_spartan_regular);
        leagueSpartanMedium = ResourcesCompat.getFont(context, R.font.league_spartan_medium);
        leagueSpartanSemiBold = ResourcesCompat.getFont(context, R.font.league_spartan_semibold);
        leagueSpartanBold = ResourcesCompat.getFont(context, R.font.league_spartan_bold);
    }

    /**
     * Lấy instance của FontHelper (Singleton pattern)
     * 
     * @param context Application context
     * @return FontHelper instance
     */
    public static FontHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FontHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Poppins Light (300)
     * Sử dụng cho: Captions, subtle text
     */
    public Typeface getLight() {
        return poppinsLight;
    }

    /**
     * Poppins Regular (400)
     * Sử dụng cho: Body text, paragraphs
     */
    public Typeface getRegular() {
        return poppinsRegular;
    }

    /**
     * Poppins Medium (500)
     * Sử dụng cho: Subtle emphasis, labels
     */
    public Typeface getMedium() {
        return poppinsMedium;
    }

    /**
     * Poppins SemiBold (600)
     * Sử dụng cho: Headings, section titles
     */
    public Typeface getSemiBold() {
        return poppinsSemiBold;
    }

    /**
     * Poppins Bold (700)
     * Sử dụng cho: Strong emphasis, main headings
     */
    public Typeface getBold() {
        return poppinsBold;
    }

    // ========================================
    // LEAGUE SPARTAN FONTS (Auth Screens)
    // ========================================

    /**
     * League Spartan Light (300)
     * Sử dụng cho: Auth captions, subtle text
     */
    public Typeface getAuthLight() {
        return leagueSpartanLight;
    }

    /**
     * League Spartan Regular (400)
     * Sử dụng cho: Auth body text, input text
     */
    public Typeface getAuthRegular() {
        return leagueSpartanRegular;
    }

    /**
     * League Spartan Medium (500)
     * Sử dụng cho: Auth labels, links
     */
    public Typeface getAuthMedium() {
        return leagueSpartanMedium;
    }

    /**
     * League Spartan SemiBold (600)
     * Sử dụng cho: Auth buttons, emphasis
     */
    public Typeface getAuthSemiBold() {
        return leagueSpartanSemiBold;
    }

    /**
     * League Spartan Bold (700)
     * Sử dụng cho: Auth titles, headings
     */
    public Typeface getAuthBold() {
        return leagueSpartanBold;
    }
}
