package org.estgroup.phphub.ui.view.topic;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cjj.MaterialRefreshLayout;
import com.cjj.MaterialRefreshListener;
import com.kennyc.view.MultiStateView;
import com.orhanobut.logger.Logger;

import org.estgroup.phphub.R;
import org.estgroup.phphub.api.entity.element.Topic;
import org.estgroup.phphub.common.adapter.TopicItemView;
import org.estgroup.phphub.common.base.LazyFragment;
import org.estgroup.phphub.ui.presenter.TopicPresenter;

import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import icepick.State;
import io.nlopez.smartadapters.SmartAdapter;
import io.nlopez.smartadapters.adapters.RecyclerMultiAdapter;
import io.nlopez.smartadapters.utils.ViewEventListener;
import nucleus.factory.PresenterFactory;
import nucleus.factory.RequiresPresenter;

import static com.kennyc.view.MultiStateView.*;
import static org.estgroup.phphub.common.qualifier.ClickType.CLICK_TYPE_TOPIC_CLICKED;
import static org.estgroup.phphub.common.qualifier.ClickType.CLICK_TYPE_USER_CLICKED;

@RequiresPresenter(TopicPresenter.class)
public class TopicFragment extends LazyFragment<TopicPresenter> implements
        ViewEventListener<Topic>{
    public static final String TOPIC_TYPE_KEY = "topic_type";

    private boolean isPrepared;

    @State
    protected String topicType = "recent";

    @Bind(R.id.multiStateView)
    MultiStateView multiStateView;

    @Bind(R.id.refresh)
    MaterialRefreshLayout refreshView;

    @Bind(R.id.recycler_view)
    RecyclerView topicListView;

    RecyclerMultiAdapter adapter;

    @Override
    protected void injectorPresenter() {
        super.injectorPresenter();
        final PresenterFactory<TopicPresenter> superFactory = super.getPresenterFactory();
        setPresenterFactory(new PresenterFactory<TopicPresenter>() {
            @Override
            public TopicPresenter createPresenter() {
                TopicPresenter presenter = superFactory.createPresenter();
                getApiComponent().inject(presenter);
                return presenter;
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.topic_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null && !TextUtils.isEmpty(getArguments().getString(TOPIC_TYPE_KEY))) {
            topicType = getArguments().getString(TOPIC_TYPE_KEY);
        }

        topicListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = SmartAdapter.empty()
                .map(Topic.class, TopicItemView.class)
                .listener(this)
                .into(topicListView);

        refreshView.setMaterialRefreshListener(new MaterialRefreshListener() {
            @Override
            public void onRefresh(MaterialRefreshLayout materialRefreshLayout) {
                getPresenter().refresh(topicType);
            }

            @Override
            public void onRefreshLoadMore(MaterialRefreshLayout materialRefreshLayout) {
                super.onRefreshLoadMore(materialRefreshLayout);
                getPresenter().nextPage(topicType);
            }
        });

        isPrepared = true;
        lazyLoad();
    }

    @Override
    protected void lazyLoad() {
        if(!isPrepared || !isVisible) {
            return;
        }

        if (!canLoadData(multiStateView, adapter)) {
            return;
        }

        multiStateView.setViewState(VIEW_STATE_LOADING);
        refreshView.autoRefresh();
    }

    public void onChangeItems(List<Topic> topics, int pageIndex) {
        if (pageIndex == 1) {
            adapter.setItems(topics);
            multiStateView.setViewState(VIEW_STATE_CONTENT);
            refreshView.finishRefresh();
        } else {
            adapter.addItems(topics);
            refreshView.finishRefreshLoadMore();
        }
    }

    public void onNetworkError(Throwable throwable, int pageIndex) {
        Logger.e(throwable.getMessage());
        if (pageIndex == 1) {
            multiStateView.setViewState(VIEW_STATE_ERROR);
        }
    }

    @Override
    public void onViewEvent(int actionId, Topic topic, int position, View view) {
        switch (actionId) {
            case CLICK_TYPE_TOPIC_CLICKED:
                navigator.navigateToTopicDetails(getActivity(), topic.getId());
                break;

            case CLICK_TYPE_USER_CLICKED:
                navigator.navigateToUserSpace(getActivity(), topic.getUserId());
                break;
        }
    }

    @OnClick(R.id.retry)
    public void retry() {
        multiStateView.setViewState(VIEW_STATE_LOADING);
        getPresenter().refresh(topicType);
    }
}
