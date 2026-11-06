package com.gityou.app.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gityou.app.R
import com.gityou.app.ui.components.EmptyState
import com.gityou.app.ui.components.LoadMoreFooter
import com.gityou.app.ui.components.RepoCard
import com.gityou.app.ui.components.StateScreen
import com.gityou.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenRepo: (String, String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.search_title)) })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                label = { Text(stringResource(R.string.search_field_label)) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_clear),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.onQueryChanged(query) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                listOf(
                    SearchSort.BEST to stringResource(R.string.search_sort_best),
                    SearchSort.STARS to stringResource(R.string.search_sort_stars),
                    SearchSort.UPDATED to stringResource(R.string.search_sort_updated),
                ).forEach { (option, label) ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { viewModel.setSort(option) },
                        label = { Text(label) },
                    )
                }
            }

            StateScreen(state = results, onRetry = { viewModel.onQueryChanged(query) }) { ui ->
                if (!ui.searched) {
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = stringResource(R.string.search_empty_title),
                        body = stringResource(R.string.search_empty_body),
                    )
                } else if (ui.repos.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = stringResource(R.string.search_no_results_title),
                        body = stringResource(R.string.search_no_results_body, ui.query),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = Spacing.lg,
                            vertical = Spacing.md,
                        ),
                    ) {
                        itemsIndexed(ui.repos, key = { _, repo -> repo.id }) { _, repo ->
                            val ownerLogin = repo.owner?.login
                                ?: repo.fullName?.substringBefore('/') ?: ""
                            if (ownerLogin.isNotEmpty()) {
                                RepoCard(repo = repo, onClick = { onOpenRepo(ownerLogin, repo.name) })
                                Spacer(Modifier.height(Spacing.md))
                            }
                        }
                        if (ui.hasMore) {
                            item(key = "load-more") {
                                LaunchedEffect(ui.repos.size) { viewModel.loadMore() }
                                LoadMoreFooter(visible = ui.loadingMore)
                            }
                        }
                    }
                }
            }
        }
    }
}
