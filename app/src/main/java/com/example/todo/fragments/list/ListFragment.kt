package com.example.todo.fragments.list

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.todo.R
import com.example.todo.data.models.ToDoData
import com.example.todo.data.viewmodel.ToDoViewModel
import com.example.todo.databinding.FragmentListBinding
import com.example.todo.fragments.SharedViewModel
import com.example.todo.fragments.list.adapter.ListAdapter
import com.example.todo.utils.hideKeyboard
import com.example.todo.utils.observeOnce
import com.google.android.material.snackbar.Snackbar
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import kotlinx.coroutines.*

class ListFragment : Fragment(), SearchView.OnQueryTextListener {
    private var _binding: FragmentListBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private val listAdapter: ListAdapter by lazy { ListAdapter() }
    private val mToDoViewModel: ToDoViewModel by viewModels()
    private val mSharedViewModel: SharedViewModel by viewModels()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.mSharedViewModel = mSharedViewModel

        setHasOptionsMenu(true)
        hideKeyboard(requireActivity())

        setUpRecyclerView()
        mToDoViewModel.getAllData.observe(viewLifecycleOwner) {
            mSharedViewModel.isDatabaseEmpty(it)
            listAdapter.setData(it)
        }

        return binding.root
    }

    private fun setUpRecyclerView() {
        binding.recyclerView.adapter = listAdapter
        binding.recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.recyclerView.itemAnimator = SlideInUpAnimator().apply { addDuration = 300 }
        swipeToDelete(binding.recyclerView)
    }

    private fun swipeToDelete(recyclerView: RecyclerView) {
        val swipeToDeleteCallback = object: SwipeToDelete() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val itemToDelete = listAdapter.dataList[viewHolder.adapterPosition]
                mToDoViewModel.deleteItem(itemToDelete)
                listAdapter.notifyItemRemoved(viewHolder.adapterPosition)
                restoreDeletedItem(viewHolder.itemView, itemToDelete)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun restoreDeletedItem(view: View, deletedItem: ToDoData) {
        Snackbar.make(
            view,
            "Deleted '${deletedItem.title}'",
            Snackbar.LENGTH_LONG
        ).apply {
            setAction("Undo") {
                mToDoViewModel.insertData(deletedItem)
            }
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.list_fragment_menu, menu)
        val search = menu.findItem(R.id.menu_search)
        val searchView = search.actionView as? SearchView
        searchView?.isSubmitButtonEnabled = true
        searchView?.setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_delete_all -> confirmRemoval()
            R.id.menu_priority_high -> mToDoViewModel
                .sortedDataHigh
                .observe(viewLifecycleOwner) {
                    listAdapter.setData(it)
                }
            R.id.menu_priority_low -> mToDoViewModel
                .sortedDataLow
                .observe(viewLifecycleOwner) {
                    listAdapter.setData(it)
                }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmRemoval() {
        val alertDialog = AlertDialog.Builder(requireContext()).apply {
            setPositiveButton("YES") { _, _ ->
                mToDoViewModel.deleteAll()
                Toast.makeText(
                    requireContext(),
                    "Successfully Removed everything! ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            setNegativeButton("NO") { _, _ -> /*DO NOTHING*/ }
            setTitle("Delete Everything?")
            setMessage("Are you sure you want to remove: Everything?")
            create()
        }
        alertDialog.show()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if(query != null) {
            searchThroughDatabase(query)
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if(newText != null) {
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                delay(500)
                searchThroughDatabase(newText)
            }
        }
        return true
    }

    private fun searchThroughDatabase(query: String?) {
        val searchQuery = "%$query%"
        mToDoViewModel
            .searchDatabase(searchQuery)
            .observeOnce(viewLifecycleOwner) {
                listAdapter.setData(it)
            }
    }
}