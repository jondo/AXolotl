package com.example.axolotltouch;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.example.axolotltouch.AuxFunctionality.PASSPROBLEMSTATE;

public class MainActivity extends DisplayUpdateHelper {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_main_bar_layout);
        findViewById(R.id.OuterLayout).setOnTouchListener(new MainSwipeListener(this));
        findViewById(R.id.OuterLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        PS = ConstructActivity(savedInstanceState);
        if (PS.anteProblem.containsAll(PS.succProblem) && PS.succProblem.containsAll(PS.succProblem)) {
            boolean passobseve = PS.observe;
            PS = new ProblemState();
            PS.observe = passobseve;
        }
        ActivityDecorate();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable("ProblemState", PS);
    }

    @Override
    protected void onRestoreInstanceState(Bundle in) {
        super.onRestoreInstanceState(in);
        PS = in.getParcelable("ProblemState");
    }

    protected void ActivityDecorate() {
        UpdateProblemDisplay();
        RuleDisplayUpdate();
    }

    private void UpdateProblemDisplay() {
        updateProblemSideDisplay((LinearLayout) this.findViewById(R.id.LeftSideTermLayout), PS.anteProblem.toArray(new Term[1]));
        updateProblemSideDisplay((LinearLayout) this.findViewById(R.id.RightSideTermLayout), PS.succProblem.toArray(new Term[1]));
    }


    protected class MainSwipeListener extends OnSwipeTouchListener {
        MainSwipeListener(Context ctx) {
            super(ctx);
        }

        public boolean onSwipeLeft() {
            if (PS.History.size() != 0)
                try {
                    Pair<Pair<ArrayList<String>, String>, Pair<ArrayList<Pair<String, Term>>, Pair<ArrayList<Term>, Term>>> laststep = PS.History.remove(PS.History.size() - 1);
                    Pair<ArrayList<Term>, Term> rule = laststep.second.second;
                    ArrayList<Term> anteSideApply = new ArrayList<>();
                    Term succSideApply = rule.second.Dup();
                    for (Pair<String, Term> s : laststep.second.first)
                        succSideApply = succSideApply.replace(new Const(s.first), s.second);
                    if (laststep.first.first.size() != 0) {
                        if (rule.first.size() > 0) {
                            for (Term t : rule.first) anteSideApply.add(t);
                            for (int i = 0; i < anteSideApply.size(); i++)
                                for (Pair<String, Term> s : laststep.second.first)
                                    anteSideApply.set(i, anteSideApply.get(i).replace(new Const(s.first), s.second));
                        }
                        HashSet<Term> newAnteProblem = new HashSet<>(anteSideApply);
                        for (Term t : PS.anteProblem)
                            if (t.Print().compareTo(succSideApply.Print()) != 0)
                                newAnteProblem.add(t);
                        PS.anteProblem = newAnteProblem;
                    } else {

                        HashSet<Term> newSuccProblem = new HashSet<>();
                        for (Pair<String, Term> s : laststep.second.first)
                            succSideApply = succSideApply.replace(new Const(s.first), s.second);
                        newSuccProblem.add(succSideApply);
                        for (Term t : rule.first) {
                            Term temp = t.Dup();
                            for (Pair<String, Term> s : laststep.second.first)
                                temp = temp.replace(new Const(s.first), s.second);
                            anteSideApply.add(temp);
                        }
                        for (Term t : PS.succProblem) {
                            boolean wasselected = false;
                            for (Term s : anteSideApply)
                                if (t.Print().compareTo(s.Print()) == 0) wasselected = true;
                            if (!wasselected) newSuccProblem.add(t);
                        }
                        PS.succProblem = newSuccProblem;
                    }
                } catch (NullPointerException e) {
                    Toast.makeText(MainActivity.this, "Problems accessing History", Toast.LENGTH_SHORT).show();
                    return true;
                }
            PS.anteSelectedPositions = new ArrayList<>();
            PS.succSelectedPosition = "";
            PS.subPos = -1;
            PS.anteCurrentRule = new ArrayList<>();
            PS.anteCurrentRule.add(Const.HoleSelected);
            PS.Substitutions = new ArrayList<>();
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.putExtra(PASSPROBLEMSTATE, PS);
            MainActivity.this.startActivity(intent);
            MainActivity.this.finish();
            Toast.makeText(MainActivity.this, "Rule Application Undone!", Toast.LENGTH_SHORT).show();
            return true;
        }

        public boolean onSwipeRight() {
            ProblemState PS = MainActivity.this.PS;
            Intent intent;
            try {
                if (PS.succCurrentRule.getSym().compareTo(Const.HoleSelected.getSym()) != 0) {
                    if (PS.succSelectedPosition.compareTo("") != 0) {
                        Term succTerm = getTermByString(PS.succSelectedPosition, PS.succProblem);
                        if (succTerm != null) {
                            PS.Substitutions = TermHelper.varTermMatch(succTerm, PS.succCurrentRule, PS);
                            HashSet<String> occurences = new HashSet<>();
                            ArrayList<Pair<String, Term>> subCleaned = new ArrayList<>();
                            for (Pair<String, Term> p : PS.Substitutions)
                                if (!occurences.contains(p.first)) {
                                    subCleaned.add(p);
                                    occurences.add(p.first);
                                }
                            PS.Substitutions = subCleaned;
                            HashSet<String> singleSide = new HashSet<>();
                            for (Term t : PS.anteCurrentRule) {
                                HashSet<String> vars = PS.VarList(t);
                                for (String s : vars)
                                    if (!PS.VarList(PS.succCurrentRule).contains(s))
                                        singleSide.add(s);
                            }
                            for (String s : singleSide)
                                PS.Substitutions.add(new Pair<>(s, Const.HoleSelected.Dup()));
                            PS.subPos = 0;
                            if (!PS.observe)
                                while (PS.subPos < PS.Substitutions.size() && !PS.Substitutions.get(PS.subPos).second.contains(Const.HoleSelected))
                                    PS.subPos++;
                            if (PS.subPos < PS.Substitutions.size() && PS.Substitutions.get(PS.subPos).second.contains(Const.HoleSelected))
                                intent = new Intent(MainActivity.this, TermConstructActivity.class);
                            else if (PS.subPos < PS.Substitutions.size())
                                intent = new Intent(MainActivity.this, MatchDisplayActivity.class);
                            else {
                                MainActivity.this.swipeRightProblemStateUpdate();
                                intent = new Intent(MainActivity.this, MainActivity.class);
                            }
                            if (PS.subPos != -1)
                                Toast.makeText(MainActivity.this, "Substitution for " + PS.Substitutions.get(PS.subPos).first, Toast.LENGTH_SHORT).show();
                            intent.putExtra(PASSPROBLEMSTATE, PS);
                            MainActivity.this.startActivity(intent);
                            MainActivity.this.finish();
                        } else
                            Toast.makeText(MainActivity.this, "Rule not applicable", Toast.LENGTH_SHORT).show();
                    } else if (PS.anteSelectedPositions.size() != 0) {
                        ArrayList<Term> anteterm = new ArrayList<>();
                        for (String s : PS.anteSelectedPositions) {
                            Term temp = getTermByString(s, PS.anteProblem);
                            if (temp != null) anteterm.add(temp);
                        }
                        if (anteterm.size() == PS.anteSelectedPositions.size()) {
                            ArrayList<Pair<Term, Term>> matchings = matchAnteProblemRule(anteterm, PS.anteCurrentRule);
                            if (matchings != null || PS.anteCurrentRule.size() == 0) {
                                PS.Substitutions = new ArrayList<>();
                                if (PS.anteCurrentRule.size() != 0)
                                    for (Pair<Term, Term> match : matchings)
                                        PS.Substitutions.addAll(TermHelper.varTermMatch(match.second, match.first, PS));
                                HashSet<String> occurences = new HashSet<>();
                                ArrayList<Pair<String, Term>> subCleaned = new ArrayList<>();
                                for (Pair<String, Term> p : PS.Substitutions)
                                    if (!occurences.contains(p.first)) {
                                        subCleaned.add(p);
                                        occurences.add(p.first);
                                    }
                                PS.Substitutions = subCleaned;
                                HashSet<String> vars = PS.VarList(PS.succCurrentRule);
                                for (Pair<String, Term> sub : PS.Substitutions)
                                    if (vars.contains(sub.first)) vars.remove(sub.first);
                                for (String s : vars)
                                    PS.Substitutions.add(new Pair<>(s, Const.HoleSelected.Dup()));
                                PS.subPos = 0;
                                if (!PS.observe)
                                    while (PS.subPos < PS.Substitutions.size() && !PS.Substitutions.get(PS.subPos).second.contains(Const.HoleSelected))
                                        PS.subPos++;
                                if (PS.subPos < PS.Substitutions.size() && PS.Substitutions.get(PS.subPos).second.contains(Const.HoleSelected))
                                    intent = new Intent(MainActivity.this, TermConstructActivity.class);
                                else if (PS.subPos < PS.Substitutions.size())
                                    intent = new Intent(MainActivity.this, MatchDisplayActivity.class);
                                else {
                                    MainActivity.this.swipeRightProblemStateUpdate();
                                    intent = new Intent(MainActivity.this, MainActivity.class);
                                }
                                if (PS.subPos != -1)
                                    Toast.makeText(MainActivity.this, "Substitution for " + PS.Substitutions.get(PS.subPos).first, Toast.LENGTH_SHORT).show();
                                intent.putExtra(PASSPROBLEMSTATE, PS);
                                MainActivity.this.startActivity(intent);
                                MainActivity.this.finish();
                            } else
                                Toast.makeText(MainActivity.this, "Rule not applicable", Toast.LENGTH_SHORT).show();
                        } else
                            Toast.makeText(MainActivity.this, "Rule not applicable", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(MainActivity.this, "Select a Side of the Problem ", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(MainActivity.this, "Select a Rule", Toast.LENGTH_SHORT).show();
            } catch (NullPointerException e) {
                Toast.makeText(MainActivity.this, "Problems Substituting", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        //assumes a unique pairing
        private ArrayList<Pair<Term, Term>> matchAnteProblemRule(ArrayList<Term> problem, ArrayList<Term> rule) {
            ProblemState PS = MainActivity.this.PS;
            if (rule.size() > 0) {
                for (Term s : problem) {
                    HashMap<String, Term> varMatching = new HashMap<>();
                    varMatching.putAll(TermHelper.varTermMatchMap(s, rule.get(0), PS));
                    if (varMatching.size() == PS.VarList(rule.get(0)).size()) {
                        ArrayList<Term> internalProblem = new ArrayList<>(problem);
                        internalProblem.remove(s);
                        ArrayList<Pair<Term, Term>> matches = matchHelper(internalProblem, rule, 1, varMatching);
                        if (matches != null) {
                            matches.add(new Pair(rule.get(0), s));
                            return matches;
                        }
                    }
                }
            }
            return null;

        }

        private ArrayList<Pair<Term, Term>> matchHelper(ArrayList<Term> problem, ArrayList<Term> rule, int pos, HashMap<String, Term> sub) {
            if (pos == rule.size()) return new ArrayList<>();
            else {
                ProblemState PS = MainActivity.this.PS;
                for (Term s : problem) {
                    HashMap<String, Term> varMatching = new HashMap<>();
                    HashMap<String, Term> retsub = new HashMap<>(sub);
                    varMatching.putAll(TermHelper.varTermMatchMap(s, rule.get(pos), PS));
                    boolean mismatch = false;
                    for (String var : sub.keySet()) {
                        if (varMatching.keySet().contains(var) && varMatching.get(var).Print().compareTo(sub.get(var).Print()) != 0)
                            mismatch = true;
                        else if (!varMatching.keySet().contains(var))
                            retsub.put(var, varMatching.get(var));
                    }
                    if (varMatching.size() == PS.VarList(rule.get(pos)).size() && !mismatch) {
                        ArrayList<Term> internalProblem = new ArrayList<>(problem);
                        internalProblem.remove(s);
                        ArrayList<Pair<Term, Term>> matches = matchHelper(internalProblem, rule, pos + 1, retsub);
                        if (matches != null) {
                            matches.add(new Pair(rule.get(pos), s));
                            return matches;
                        }
                    }
                }
                return null;
            }
        }
        private Term getTermByString(String succSelectedPosition, HashSet<Term> succProblem) {
            for (Term t : succProblem)
                if (t.Print().compareTo(succSelectedPosition) == 0) return t.Dup();
            return null;
        }
    }




}
