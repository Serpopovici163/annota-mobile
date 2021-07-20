package com.example.annotamobile.ui.library;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Network;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.annotamobile.R;
import com.example.annotamobile.ui.NetworkIO;

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

public class LibraryFragment extends Fragment implements View.OnClickListener {

    ListView resultView;
    ArrayList<SearchResult> searchResultArrayList = new ArrayList<>();
    SearchResultAdapter adapter;
    View parentView;

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        parentView = inflater.inflate(R.layout.fragment_library, container, false);

        Button search_button = parentView.findViewById(R.id.search_criteria_button);
        search_button.setOnClickListener(this);

        Button sort_button = parentView.findViewById(R.id.sort_criteria_button);
        sort_button.setOnClickListener(this);

        //initialize empty adapter
        adapter = new SearchResultAdapter(requireContext(), R.layout.library_list_item, searchResultArrayList);
        resultView = parentView.findViewById(R.id.result_listView);
        resultView.setAdapter(adapter);

        return parentView;
    }

    @Override
    public void onClick(View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        AlertDialog dialog;
        View view;
        Spinner mainSpinner;

        switch (v.getId()) {
            case R.id.search_criteria_button: //popup a window for user to enter search criteria
                alert = new AlertDialog.Builder(getContext());
                view = getLayoutInflater().inflate(R.layout.search_popup, null);
                View[] childView = {null};
                mainSpinner = view.findViewById(R.id.criteria_type_dropdown);
                LinearLayout search_data = view.findViewById(R.id.search_data_placeholder);

                alert.setTitle(R.string.search_popup_title);
                alert.setView(view);

                alert.setPositiveButton(R.string.search_popup_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        adapter.clear();

                        //run search
                        NetworkIO networkIO = new NetworkIO();
                        String[] data = {"","",""};
                        switch (mainSpinner.getSelectedItemPosition()) {
                            case 0: //content
                                EditText text = childView[0].findViewById(R.id.search_by_content_content);
                                data[0] = text.getText().toString();
                                break;
                            case 1: //category
                                AutoCompleteTextView cat1 = childView[0].findViewById(R.id.cat1_content);
                                AutoCompleteTextView cat2 = childView[0].findViewById(R.id.cat2_content);
                                AutoCompleteTextView cat3 = childView[0].findViewById(R.id.cat3_content);
                                data[0] = cat1.getText().toString();
                                data[1] = cat2.getText().toString();
                                data[2] = cat3.getText().toString();
                                break;
                            case 2: //date
                                break;
                        }
                        networkIO.search(requireContext(), mainSpinner.getSelectedItemPosition(), data, new NetworkIO.NetworkIOListener() {
                            @Override
                            public void onSuccess(@Nullable String[] data) {
                                //populate search_data view
                                if (data != null) {
                                    String[] child_data;
                                    SearchResult buffer;

                                    for (int i = 1; i < data.length; i++) {
                                        //inflate child view and obtain all of its components
                                        child_data = data[i].split("&");
                                        Bitmap icon = networkIO.stringToBitmap(child_data[8]);
                                        buffer = new SearchResult(child_data[0], child_data[1], child_data[2], child_data[3], child_data[4], child_data[5], child_data[6], child_data[7], icon);
                                        searchResultArrayList.add(buffer);
                                    }
                                } else {
                                    Toast.makeText(getContext(), R.string.no_search_results, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        adapter.notifyDataSetChanged();

                        resultView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                //change layout and show full res picture with content
                                networkIO.getItemPicture(requireContext(), adapter.getItem(position).getDate_time(), new NetworkIO.NetworkIOListener() {
                                    @Override
                                    public void onSuccess(@Nullable String[] data) {
                                        //index 1 will be the picture but we need to convert this to string
                                        Bitmap thumbnail = networkIO.stringToBitmap(data[1]);

                                        //initialize all views in the single item layout
                                        ScrollView singleItemView = parentView.findViewById(R.id.single_item_view);
                                        ImageView singleItemThumbnail = parentView.findViewById(R.id.single_item_thumbnail);
                                        EditText singleItemTitle = parentView.findViewById(R.id.single_item_title);
                                        TextView singleItemDate = parentView.findViewById(R.id.single_item_date);
                                        EditText singleItemContent = parentView.findViewById(R.id.single_item_content);
                                        EditText singleItemComments = parentView.findViewById(R.id.single_item_comments);
                                        AutoCompleteTextView singleItemCat1 = parentView.findViewById(R.id.single_item_cat1);
                                        AutoCompleteTextView singleItemCat2 = parentView.findViewById(R.id.single_item_cat2);
                                        AutoCompleteTextView singleItemCat3 = parentView.findViewById(R.id.single_item_cat3);

                                        //assign the ones that can be easily assigned
                                        singleItemThumbnail.setImageBitmap(thumbnail);
                                        singleItemTitle.setText(adapter.getItem(position).getName());
                                        singleItemDate.setText(adapter.getItem(position).getDate_time());
                                        singleItemContent.setText(adapter.getItem(position).getContent());
                                        singleItemComments.setText(adapter.getItem(position).getComments());

                                        //obtain category list from server to initialize dropdown menu for category dialogs
                                        //this part will also update the current selection to the current item's category
                                        networkIO.getCatList(requireContext(), new NetworkIO.NetworkIOListener() {
                                            @Override
                                            public void onSuccess(@Nullable String[] data) {
                                                ArrayAdapter<String> cat1_adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, data[0].split(","));
                                                ArrayAdapter<String> cat2_adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, data[1].split(","));
                                                ArrayAdapter<String> cat3_adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, data[2].split(","));

                                                singleItemCat1.setAdapter(cat1_adapter);
                                                singleItemCat2.setAdapter(cat2_adapter);
                                                singleItemCat3.setAdapter(cat3_adapter);

                                                singleItemCat1.setOnTouchListener(new View.OnTouchListener() {
                                                    @Override
                                                    public boolean onTouch(View v, MotionEvent event) {
                                                        singleItemCat1.showDropDown();
                                                        return false;
                                                    }
                                                });

                                                singleItemCat2.setOnTouchListener(new View.OnTouchListener() {
                                                    @Override
                                                    public boolean onTouch(View v, MotionEvent event) {
                                                        singleItemCat2.showDropDown();
                                                        return false;
                                                    }
                                                });

                                                singleItemCat3.setOnTouchListener(new View.OnTouchListener() {
                                                    @Override
                                                    public boolean onTouch(View v, MotionEvent event) {
                                                        singleItemCat3.showDropDown();
                                                        return false;
                                                    }
                                                });

                                                singleItemCat1.setText(adapter.getItem(position).getCat1());
                                                singleItemCat2.setText(adapter.getItem(position).getCat2());
                                                singleItemCat3.setText(adapter.getItem(position).getCat3());

                                            }
                                        });

                                        //finish off by initializing buttons
                                        //first one send updated values to the server and second simply exits
                                        Button editButton = parentView.findViewById(R.id.single_item_edit_button);
                                        editButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                int cumulativeResult = 0;
                                                //check every field to see if any changes occurred
                                                //for every change, add 1 to cumulativeResult and at the end we can use that integer to see if any changes occurred
                                                if (!Objects.equals(adapter.getItem(position).getName(), singleItemTitle.getText().toString())) {
                                                    adapter.getItem(position).setName(singleItemTitle.getText().toString());
                                                    cumulativeResult++;
                                                }
                                                if (!Objects.equals(adapter.getItem(position).getContent(), singleItemContent.getText().toString())) {
                                                    adapter.getItem(position).setContent(singleItemContent.getText().toString());
                                                    cumulativeResult++;
                                                }
                                                if (!Objects.equals(adapter.getItem(position).getComments(), singleItemComments.getText().toString())) {
                                                    adapter.getItem(position).setComments(singleItemComments.getText().toString());
                                                    cumulativeResult++;
                                                }
                                                if (!Objects.equals(adapter.getItem(position).getCat1(), singleItemCat1.getText().toString())) {
                                                    adapter.getItem(position).setCat1(singleItemCat1.getText().toString());
                                                    cumulativeResult++;
                                                }
                                                if (!Objects.equals(adapter.getItem(position).getCat2(), singleItemCat2.getText().toString())) {
                                                    adapter.getItem(position).setCat2(singleItemCat2.getText().toString());
                                                    cumulativeResult++;
                                                }
                                                if (!Objects.equals(adapter.getItem(position).getCat3(), singleItemCat3.getText().toString())) {
                                                    adapter.getItem(position).setCat3(singleItemCat3.getText().toString());
                                                    cumulativeResult++;
                                                }

                                                //now check to see if any have been changed and if so send an update to the server
                                                if (cumulativeResult != 0)
                                                    networkIO.updateItem(requireContext(),
                                                            adapter.getItem(position).getId(),
                                                            singleItemTitle.getText().toString(),
                                                            singleItemContent.getText().toString(),
                                                            singleItemComments.getText().toString(),
                                                            singleItemCat1.getText().toString(),
                                                            singleItemCat2.getText().toString(),
                                                            singleItemCat3.getText().toString(), new NetworkIO.NetworkIOListener() {
                                                                @Override
                                                                public void onSuccess(@Nullable String[] data) {
                                                                    singleItemView.setVisibility(View.INVISIBLE);
                                                                }
                                                            });
                                                else
                                                    singleItemView.setVisibility(View.INVISIBLE);
                                            }
                                        });

                                        Button exitButton = parentView.findViewById(R.id.single_item_exit_button);
                                        exitButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                singleItemView.setVisibility(View.INVISIBLE);
                                            }
                                        });

                                        singleItemView.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        });
                    }
                });

                alert.setNegativeButton(R.string.popup_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //end fragment
                        dialog.cancel();
                    }
                });

                mainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        search_data.removeAllViews();
                        switch (position) {
                            case 0: //content
                                childView[0] = getLayoutInflater().inflate(R.layout.search_popup_content, search_data);
                                break;
                            case 1: //category
                                childView[0] = getLayoutInflater().inflate(R.layout.search_popup_category, search_data);
                                AutoCompleteTextView cat1 = childView[0].findViewById(R.id.cat1_content);
                                AutoCompleteTextView cat2 = childView[0].findViewById(R.id.cat2_content);
                                AutoCompleteTextView cat3 = childView[0].findViewById(R.id.cat3_content);

                                //get category list to initialize dropdown menu for each autocompletetextview
                                NetworkIO networkIO = new NetworkIO();
                                networkIO.getCatList(requireContext(), new NetworkIO.NetworkIOListener() {
                                    @Override
                                    public void onSuccess(@Nullable String[] data) {
                                        cat1.setAdapter(new ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data[0].split(",")));
                                        cat2.setAdapter(new ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data[1].split(",")));
                                        cat3.setAdapter(new ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data[2].split(",")));

                                    }
                                });

                                //setup touch listener so the dropdowns popup when needed
                                cat1.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        cat1.showDropDown();
                                        return false;
                                    }
                                });

                                cat2.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        cat2.showDropDown();
                                        return false;
                                    }
                                });

                                cat3.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        cat3.showDropDown();
                                        return false;
                                    }
                                });
                                break;
                            case 2: //date
                                childView[0] = getLayoutInflater().inflate(R.layout.search_popup_date, search_data);
                                break;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        search_data.removeAllViews();
                    }
                });

                //show popup and make sure keyboard pops up
                dialog = alert.create();
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface badDialog) {
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                    }
                });

                dialog.show();

                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                break;
            case R.id.sort_criteria_button: //popup once again
                alert = new AlertDialog.Builder(getContext());
                view = getLayoutInflater().inflate(R.layout.sort_popup, null);
                mainSpinner = view.findViewById(R.id.sort_type_dropdown);

                alert.setTitle(R.string.sort_popup_title);
                alert.setView(view);

                alert.setPositiveButton(R.string.sort_popup_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Comparator<SearchResult> comparator = null;
                        switch (mainSpinner.getSelectedItemPosition()) {
                            case 0: //sort by name
                                comparator = new Comparator<SearchResult>() {
                                    @Override
                                    public int compare(SearchResult o1, SearchResult o2) {
                                        return o1.getName().compareTo(o2.getName());
                                    }
                                };
                                break;
                            case 1: //sort by date
                                comparator = new Comparator<SearchResult>() {
                                    @Override
                                    public int compare(SearchResult o1, SearchResult o2) {
                                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                                        Date d1 = null;
                                        Date d2 = null;
                                        try {
                                            d1 = formatter.parse(o1.getDate_time());
                                            d2 = formatter.parse(o2.getDate_time());
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            return 0;
                                        }
                                        assert d1 != null;
                                        return d1.compareTo(d2);
                                    }
                                };
                                break;
                        }
                        Collections.sort(searchResultArrayList, comparator);
                        adapter.notifyDataSetChanged();
                    }
                });

                alert.setNegativeButton(R.string.popup_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //end fragment
                        dialog.cancel();
                    }
                });

                //show popup
                dialog = alert.create();
                dialog.show();
        }
    }
}