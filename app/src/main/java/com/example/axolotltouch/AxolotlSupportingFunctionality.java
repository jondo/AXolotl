package com.example.axolotltouch;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import static com.example.axolotltouch.AxolotlMessagingAndIO.PASSPROBLEMSTATE;

/**
 * This abstract class extends the abstract class AxolotlSupportingListenersAndMethods with methods essential for
 * the main activities of Axolotl.
 *
 * @author David M. Cerna
 */
public abstract class AxolotlSupportingFunctionality extends AxolotlSupportingListenersAndMethods {
    /**
     * This is the switch located in the navigation menu which turns observation mode on and off
     */
    androidx.appcompat.widget.SwitchCompat switcher;
    androidx.appcompat.widget.AppCompatSeekBar seeker;


    /**
     * This is an abstract method used to decorate the layout of the activities implementing this
     * abstract class.
     * @author David M. Cerna
     */
    protected abstract void ActivityDecorate();

    /**
     * Each activity implementing AxolotlSupportingFunctionality must construct a particular toolbar and
     * navigation menu as well as read the problem state and state the switch in the appropriate
     * state.
     * @author David M. Cerna
     * @param in The Bundle containing the intent from the activity which requested the creation of
     *           current activity
     * @return The problem state after the necessary changes.
     */
    protected ProblemState ConstructActivity(Bundle in) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.Drawer);
        addMenulisteners();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        Intent intent = getIntent();
        PS = (PS == null) ? constructProblemState(in, intent) : PS;
        switcher = findViewById(R.id.observeswitchformenu);
        switcher.setChecked(PS.observe);
        switcher.setOnCheckedChangeListener(new ObservationListener());
        seeker = findViewById(R.id.Adjusttextseeker);
        seeker.setProgress(PS.textSize);
        return PS;
    }

    /**
     * If the activity implementing AxolotlSupportingFunctionality has a variety of layouts depending on the
     * problem state it may be necessary to extract the problem state from the Bundle prior to
     * constructing the activity.
     * @param in The Bundle containing the intent from the activity which requested the creation of
     *           current activity
     * @param tent The intent sent along with the Bundle.
     * @return The problem state after the necessary changes.
     */
    protected ProblemState constructProblemState(Bundle in, Intent tent) {
        if (tent.hasExtra(PASSPROBLEMSTATE)) PS = tent.getParcelableExtra(PASSPROBLEMSTATE);
        else if (in != null && in.containsKey("ProblemState"))
            PS = in.getParcelable("ProblemState");
        else PS = new ProblemState();
        return PS;
    }

    /**
     * Being that the Navigation menu includes non-standard components we avoid using the standard
     * listeners associated with the navigation menu and instead add the following
     * MenuOnClickListener to each menu item.
     * @author David M. Cerna
     */
    protected void addMenulisteners() {
        LinearLayout ll = findViewById(R.id.nonclassicbuttonlayout);
        ll.setOnClickListener(new MenuOnClickListener());
        ll = findViewById(R.id.classicbuttonlayout);
        ll.setOnClickListener(new MenuOnClickListener());
        ll = findViewById(R.id.TermMatchingbuttonlayout);
        ll.setOnClickListener(new MenuOnClickListener());
        ll = findViewById(R.id.Proofbuttonlayout);
        ll.setOnClickListener(new MenuOnClickListener());
        ll = findViewById(R.id.problembuttonlayout);
        ll.setOnClickListener(new MenuOnClickListener());
        SeekBar seek = findViewById(R.id.Adjusttextseeker);
        seek.setOnSeekBarChangeListener(new TextSizeChangeListener());
    }

    /**
     * Swiping right results in a few standard changes to the problem state which are handled by the
     * following function. It is important that the changes occur prior to creating the new activity
     * which will handle the changes to the problem state.
     * @author David M. Cerna
     */
    protected void swipeRightProblemStateUpdate() {
        if (PS.currentRule.argument.getSym().compareTo(new Rule().argument.getSym()) != 0) {
            PS.History.add(new State(PS.succSelectedPosition, PS.Substitutions, PS.currentRule));
        }
        HashSet<Term> newProblemsucc = PS.Substitutions.apply(PS.currentRule.Conclusions);
        for (Term t : PS.problem)
            if (t.Print().compareTo(PS.succSelectedPosition) != 0) newProblemsucc.add(t);
        PS.problem = newProblemsucc;
        PS.succSelectedPosition = "";
        PS.subPos = -1;
        PS.currentRule = new Rule();
        PS.Substitutions = new Substitution();
        PS.SubHistory = new HashMap<>();
        if (PS.problem.isEmpty()) PS.problem.add(Const.Empty.Dup());
        if ((PS.problem.size() == 0 || PS.problem.iterator().next().getSym().compareTo("∅") == 0)) {
            Toast.makeText(AxolotlSupportingFunctionality.this, "Congratulations! Problem Solved! ", Toast.LENGTH_SHORT).show();
            PS.mainActivityState = 2;
        } else
            Toast.makeText(AxolotlSupportingFunctionality.this, "Rule Applied", Toast.LENGTH_SHORT).show();
    }

    /**
     * When loading a file from the system or from the internal problem library this function captures
     * the result of the request and handles it.
     * @author David M. Cerna
     * @param requestCode The request sent to a system level activity.
     * @param resultCode The result of the request.
     * @param data What was retrieved from the request.
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AxolotlMessagingAndIO.READ_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                InputStream inputStream;
                try {
                    inputStream = getContentResolver().openInputStream(data.getData());
                    PS = AxolotlMessagingAndIO.loadFile(inputStream, new File(Objects.requireNonNull(data.getData().getPath())).getName(), this);
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, "Unable to load file", Toast.LENGTH_SHORT).show();                }
            }
            if (PS != null) ActivityDecorate();
            else {
                PS = new ProblemState();
                Toast.makeText(this, "Unable to load file", Toast.LENGTH_SHORT).show();
            }
        } else super.onActivityResult(requestCode,resultCode,data);

    }

    /**
     * This method construct scrollable text views in a uniform way allowing a consistent look across
     * all activities.
     * @author David M. Cerna
     * @param text The text to be displayed by the text view.
     * @param lis The listener associated with clicking on the view.
     * @param ctx The activity associated with the text view.
     * @param gravity whether or not gravity should be centered within the text view and its scrolling
     *                elements.
     * @return The scroll view containing the text view.
     */
    public HorizontalScrollView scrollTextSelectConstruct(String text, View.OnClickListener lis, View.OnLongClickListener longLis, Context ctx, boolean gravity) {
        TextView TermText = new TextView(ctx);
        TermText.setTextSize(PS.textSize);
        TermText.setText(Html.fromHtml(text));
        if (gravity) TermText.setGravity(Gravity.CENTER);
        TermText.setFreezesText(true);
        TermText.setTextColor(Color.BLACK);
        TermText.setBackgroundColor(Color.WHITE);
        TermText.setLayoutParams(new FrameLayout.LayoutParams(((int) TermText.getPaint().measureText(TermText.getText().toString()) + 20), FrameLayout.LayoutParams.WRAP_CONTENT));
        if (lis != null) TermText.setOnClickListener(lis);
        if (longLis != null) {
            TermText.setLongClickable(true);
            TermText.setOnLongClickListener(longLis);
        }
        LinearLayout scrollLayout = new LinearLayout(this);
        scrollLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.NO_GRAVITY));//(gravity) ? Gravity.CENTER : Gravity.NO_GRAVITY));
        scrollLayout.setOrientation(LinearLayout.VERTICAL);
        scrollLayout.addView(TermText);
        HorizontalScrollView HScroll = new HorizontalScrollView(this);
        HScroll.setScrollbarFadingEnabled(false);
        HScroll.setScrollBarDefaultDelayBeforeFade(0);
        HScroll.setHorizontalScrollBarEnabled(true);
        HScroll.addView(scrollLayout);
        HScroll.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, (gravity) ? Gravity.CENTER : Gravity.NO_GRAVITY));

        return HScroll;
    }

    /**
     * The problem is a sequence of terms which are displayed within a vertically oriented linear layout.
     * We add this sequence of terms to the problem display using the uniform text view created by the
     * scrollTextSelectConstruct() method.
     * @author David M. Cerna
     * @param sl The linear layout concerning the problem as text views
     * @param t The sequence of terms representing the problem
     */
    protected void updateProblemSideDisplay(LinearLayout sl, Term[] t) {
        sl.removeAllViewsInLayout();
        for (Term term : t)
            sl.addView(scrollTextSelectConstruct(term.Print(), new SideSelectionListener(), null, this, true));
    }

    /**
     * The future problem state is a sequence of terms which are displayed within a vertically oriented linear layout.
     * We add this sequence of terms to the future problem state display using the uniform text view created by the
     * scrollTextSelectConstruct() method.
     * @author David M. Cerna
     * @param sl The linear layout concerning the future problem state as text views
     * @param t The sequence of terms representing the future problem state
     */
    protected void updatefutureProblemSideDisplay(LinearLayout sl, Term[] t) {
        sl.removeAllViewsInLayout();
        for (Term term : t) {
            term.normalize(PS.Variables);
            sl.addView(scrollTextSelectConstruct(term.Print(), null, null, this, false));
        }
    }

    /**
     * Rules are a sequence of term pairs which are displayed within a vertically oriented linear layout.
     * We add this sequence of term pairs to the rule display using the uniform text view created by the
     * scrollTextSelectConstruct() method.
     * @author David M. Cerna
     */
    protected void RuleDisplayUpdate() {
        LinearLayout RLVV = this.findViewById(R.id.RuleListVerticalLayout);
        RLVV.removeAllViewsInLayout();
        for (int i = 0; i < PS.Rules.size(); i++)
            RLVV.addView(scrollTextSelectConstruct(Rule.RuleTermsToString(PS.Rules.get(i)),
                    new AxolotlSupportingFunctionality.RuleSelectionListener(),
                    new AxolotlSupportingFunctionality.RuleViewListener(),
                    this, false));
    }


}
