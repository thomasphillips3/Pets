package com.thomasphillips3.pets;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.amplify.generated.graphql.CreatePetMutation;
import com.amazonaws.amplify.generated.graphql.ListPetsQuery;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import type.CreatePetInput;

public class AddPetActivity extends AppCompatActivity {
    private static final String TAG = AddPetActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        Button buttonAddItem = findViewById(R.id.button_save);
        buttonAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });
    }

    private void save() {
        final String name = ((EditText) findViewById(R.id.editText_name)).getText().toString();
        final String description = ((EditText) findViewById(R.id.editText_description)).getText().toString();

        CreatePetInput input = CreatePetInput.builder()
                .name(name)
                .description(description)
                .build();

        CreatePetMutation addPetMutation = CreatePetMutation.builder()
                .input(input)
                .build();

        ClientFactory.appSyncClient().mutate(addPetMutation)
                .enqueue(mutateCallback);

        ClientFactory.appSyncClient().mutate(addPetMutation)
                .refetchQueries(ListPetsQuery.builder().build())
                .enqueue(mutateCallback);

        addPetOffline(input);
    }

    private void addPetOffline(CreatePetInput input) {
        final CreatePetMutation.CreatePet expected = new CreatePetMutation.CreatePet(
                "Pet",
                UUID.randomUUID().toString(),
                input.name(),
                input.description());

        final AWSAppSyncClient awsAppSyncClient = ClientFactory.appSyncClient();
        final ListPetsQuery listEventsQuery = ListPetsQuery.builder().build();

        awsAppSyncClient.query(listEventsQuery)
                .responseFetcher(AppSyncResponseFetchers.CACHE_ONLY)
                .enqueue(new GraphQLCall.Callback<ListPetsQuery.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<ListPetsQuery.Data> response) {
                        List<ListPetsQuery.Item> items = new ArrayList<>();
                        if (response.data() != null) {
                            items.addAll(response.data().listPets().items());
                        }
                        items.add(new ListPetsQuery.Item(expected.__typename(),
                                 expected.id(),
                                 expected.name(),
                                 expected.description()));
                        ListPetsQuery.Data data = new ListPetsQuery.Data(new ListPetsQuery.ListPets("ModelPetConnection", items, null));
                        awsAppSyncClient.getStore().write(listEventsQuery, data).enqueue(null);
                        Log.d(TAG, "Wrote to local store while offline.");

                        finishIfOffline();
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e(TAG, "Failed to update event query list.", e);
                    }
                });
    }

    private void finishIfOffline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Log.d(TAG, "Offline. Returning to MainActivity.");
            finish();
        }
    }

    private GraphQLCall.Callback<CreatePetMutation.Data> mutateCallback = new GraphQLCall.Callback<CreatePetMutation.Data>() {
        @Override
        public void onResponse(@Nonnull final Response<CreatePetMutation.Data> response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AddPetActivity.this, "Added pet", Toast.LENGTH_SHORT).show();
                    AddPetActivity.this.finish();
                }
            });
        }

        @Override
        public void onFailure(@Nonnull final ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("", "Failed to perform AddPetMutation", e);
                    Toast.makeText(AddPetActivity.this, "Failed to add pet", Toast.LENGTH_SHORT).show();
                    AddPetActivity.this.finish();
                }
            });
        }
    };
}
